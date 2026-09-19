from fastapi import FastAPI, UploadFile, File
import fitz
import re
import psycopg2
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer, CrossEncoder
from google import genai
import os
import json

app = FastAPI()

client = genai.Client(
    api_key=os.getenv("GEMINI_API_KEY")
)


def get_db_connection():

    return psycopg2.connect(
        host="localhost",
        port=5432,
        database="insightrag",
        user="subhankar"
    )


def generate_answer(prompt):

    response = client.models.generate_content(
        model="gemini-2.5-flash",
        contents=prompt
    )

    text = response.text.strip()

    if text.startswith("```"):
        text = text.replace("```json", "").replace("```", "").strip()

    return json.loads(text)


def create_prompt(question, context):

    prompt = f"""
You are an AI business analyst.

Answer the user's question using ONLY the provided context.

The context may contain information from multiple documents.

Rules:
1. Do not use outside knowledge.
2. Do not invent facts.
3. Answer the question directly.
4. You may combine information from multiple documents when the question requires it.
5. Cite every important factual claim using the exact document name and page number that supports the claim.
6. Only cite documents and pages that directly support the answer.
7. If a claim is supported by multiple documents, cite all relevant documents and pages.
8. If the question requires information from multiple documents, synthesize the information into one coherent answer.
9. Do not assume that information from one document applies to another document unless the context supports it.
10. If the context does not contain enough information, say:
"The provided documents do not contain enough information to answer this question."
11. Keep the answer concise.

Return ONLY valid JSON in this format:

{{
    "answer": "Your answer here.",
    "sources": [
        {{
            "document": "document_name.pdf",
            "page": 24
        }}
    ]
}}

Context:
{context}

Question:
{question}
"""

    return prompt


def build_context(results):

    context = ""

    for result in results:

        context += f"""
[Document: {result["document"]}]
[Page: {result["page"]}]

{result["text"]}

"""

    return context


class RetrieveRequest(BaseModel):

    question: str


embedding_model = SentenceTransformer(
    "BAAI/bge-small-en-v1.5"
)

reranker = CrossEncoder(
    "cross-encoder/ms-marco-MiniLM-L-6-v2"
)


@app.get("/")
def home():

    return {
        "message": "InsightRAG AI Service is running"
    }


@app.post("/process-document")
async def process_document(file: UploadFile = File(...)):

    content = await file.read()

    pdf = fitz.open(
        stream=content,
        filetype="pdf"
    )

    pages = []

    for page_number, page in enumerate(pdf):

        text = page.get_text()

        if text.strip():

            pages.append({
                "page": page_number + 1,
                "text": text
            })

    pdf.close()

    for page in pages:

        page["text"] = re.sub(
            r'\s+',
            ' ',
            page["text"]
        ).strip()

    chunks = []

    chunk_size = 1000
    overlap = 200

    for page in pages:

        text = page["text"]
        start = 0

        while start < len(text):

            end = start + chunk_size

            chunk = text[start:end]

            if chunk.strip():

                chunks.append({
                    "page": page["page"],
                    "text": chunk
                })

            start += chunk_size - overlap

    return {
        "filename": file.filename,
        "status": "PROCESSED",
        "pages": len(pages),
        "chunks": len(chunks)
    }


@app.post("/extract-document")
async def extract_document(file: UploadFile = File(...)):

    content = await file.read()

    pdf = fitz.open(
        stream=content,
        filetype="pdf"
    )

    pages = []

    for page_number, page in enumerate(pdf):

        text = page.get_text()

        if text.strip():

            pages.append({
                "page": page_number + 1,
                "text": text
            })

    pdf.close()

    for page in pages:

        page["text"] = re.sub(
            r'\s+',
            ' ',
            page["text"]
        ).strip()

    return {
        "filename": file.filename,
        "pages": pages
    }


@app.post("/create-embeddings/{document_id}")
async def create_embeddings(
        document_id: int,
        file: UploadFile = File(...)
):

    content = await file.read()

    pdf = fitz.open(
        stream=content,
        filetype="pdf"
    )

    pages = []

    for page_number, page in enumerate(pdf):

        text = page.get_text()

        if text.strip():

            text = re.sub(
                r'\s+',
                ' ',
                text
            ).strip()

            pages.append({
                "page": page_number + 1,
                "text": text
            })

    pdf.close()

    chunks = []

    chunk_size = 1000
    overlap = 200

    for page in pages:

        text = page["text"]
        start = 0

        while start < len(text):

            end = start + chunk_size

            chunk = text[start:end]

            if chunk.strip():

                chunks.append({
                    "page": page["page"],
                    "text": chunk
                })

            start += chunk_size - overlap

    texts = [
        chunk["text"]
        for chunk in chunks
    ]

    embeddings = embedding_model.encode(
        texts,
        normalize_embeddings=True
    )

    connection = get_db_connection()

    cursor = connection.cursor()

    for i in range(len(chunks)):

        embedding = embeddings[i].tolist()

        cursor.execute(
            """
            INSERT INTO document_chunks
                (document_id, page, content, embedding)
            VALUES (%s, %s, %s, %s)
            """,
            (
                document_id,
                chunks[i]["page"],
                chunks[i]["text"],
                embedding
            )
        )

    connection.commit()

    cursor.close()
    connection.close()

    return {
        "filename": file.filename,
        "status": "EMBEDDED",
        "pages": len(pages),
        "chunks": len(chunks),
        "embedding_dimension": embeddings.shape[1]
    }


@app.post("/ask")
async def ask(request: RetrieveRequest):

    query_embedding = embedding_model.encode(
        [request.question],
        normalize_embeddings=True
    )[0].tolist()

    connection = get_db_connection()
    cursor = connection.cursor()

    cursor.execute(
        """
        SELECT
            dc.document_id,
            d.filename,
            dc.page,
            dc.content,
            1 - (dc.embedding <=> %s::vector) AS score
        FROM document_chunks dc
                 JOIN documents d
                      ON dc.document_id = d.id
        ORDER BY dc.embedding <=> %s::vector
            LIMIT 20
        """,
        (
            query_embedding,
            query_embedding
        )
    )

    rows = cursor.fetchall()

    cursor.close()
    connection.close()

    if not rows:

        return {
            "question": request.question,
            "answer": "The provided documents do not contain enough information to answer this question.",
            "sources": []
        }

    candidates = []

    for row in rows:

        candidates.append({
            "document_id": row[0],
            "document": row[1],
            "page": row[2],
            "text": row[3]
        })

    pairs = [
        [request.question, candidate["text"]]
        for candidate in candidates
    ]

    rerank_scores = reranker.predict(pairs)

    for i in range(len(candidates)):

        candidates[i]["score"] = float(
            rerank_scores[i]
        )

    candidates.sort(
        key=lambda x: x["score"],
        reverse=True
    )

    results = candidates[:8]

    context = build_context(results)

    prompt = create_prompt(
        request.question,
        context
    )

    answer = generate_answer(prompt)

    return {
        "question": request.question,
        "answer": answer["answer"],
        "sources": answer["sources"]
    }