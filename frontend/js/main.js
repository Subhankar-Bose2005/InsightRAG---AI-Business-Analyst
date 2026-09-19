const API_BASE_URL = "http://localhost:8080";

const questionInput = document.getElementById("questionInput");
const sendButton = document.getElementById("sendButton");
const chatMessages = document.getElementById("chatMessages");

const uploadButton = document.getElementById("uploadButton");
const fileInput = document.getElementById("fileInput");
const documentList = document.getElementById("documentList");


/* =========================
   Chat
   ========================= */

sendButton.addEventListener("click", askQuestion);

questionInput.addEventListener("keydown", function (event) {

    if (event.key === "Enter" && !event.shiftKey) {

        event.preventDefault();

        askQuestion();
    }
});


async function askQuestion() {

    const question = questionInput.value.trim();

    if (!question) {
        return;
    }

    addUserMessage(question);

    questionInput.value = "";

    sendButton.disabled = true;

    const loadingMessage = addLoadingMessage();

    try {

        const response = await fetch(
            `${API_BASE_URL}/rag/ask`,
            {
                method: "POST",

                headers: {
                    "Content-Type": "application/json"
                },

                body: JSON.stringify({
                    question: question
                })
            }
        );

        if (!response.ok) {
            throw new Error("Failed to get response from server");
        }

        const data = await response.json();

        loadingMessage.remove();

        addAIMessage(
            data.answer,
            data.sources
        );

    } catch (error) {

        loadingMessage.remove();

        addErrorMessage();

        console.error(error);

    } finally {

        sendButton.disabled = false;

        questionInput.focus();
    }
}


function addUserMessage(question) {

    const message = document.createElement("div");

    message.className = "message user";

    message.innerHTML = `
        <div class="message-content">

            <div class="message-label">
                You
            </div>

            <div class="message-bubble">
                ${escapeHTML(question)}
            </div>

        </div>
    `;

    chatMessages.appendChild(message);

    scrollToBottom();
}


function addAIMessage(answer, sources) {

    const message = document.createElement("div");

    message.className = "message ai";

    message.innerHTML = `
        <div class="message-content">

            <div class="message-label">
                InsightRAG
            </div>

            <div class="message-bubble">
                ${escapeHTML(answer)}
            </div>

            ${createSourcesHTML(sources)}

        </div>
    `;

    chatMessages.appendChild(message);

    scrollToBottom();
}


function createSourcesHTML(sources) {

    if (!sources || sources.length === 0) {
        return "";
    }

    let sourceCards = "";

    for (const source of sources) {

        sourceCards += `
            <div class="source-card">

                <span>📄</span>

                <span>
                    ${escapeHTML(source.document)}
                    · Page ${source.page}
                </span>

            </div>
        `;
    }

    return `
        <div class="sources">

            <div class="sources-title">
                Sources
            </div>

            ${sourceCards}

        </div>
    `;
}


function addLoadingMessage() {

    const message = document.createElement("div");

    message.className = "message ai";

    message.innerHTML = `
        <div class="message-content">

            <div class="message-label">
                InsightRAG
            </div>

            <div class="message-bubble">
                Thinking...
            </div>

        </div>
    `;

    chatMessages.appendChild(message);

    scrollToBottom();

    return message;
}


function addErrorMessage() {

    const message = document.createElement("div");

    message.className = "message ai";

    message.innerHTML = `
        <div class="message-content">

            <div class="message-label">
                InsightRAG
            </div>

            <div class="message-bubble">
                Something went wrong while processing your question.
                Please try again.
            </div>

        </div>
    `;

    chatMessages.appendChild(message);

    scrollToBottom();
}


function scrollToBottom() {

    chatMessages.scrollTo({
        top: chatMessages.scrollHeight,
        behavior: "smooth"
    });
}


/* =========================
   Document Upload
   ========================= */

uploadButton.addEventListener("click", function () {

    fileInput.click();

});


fileInput.addEventListener("change", function () {

    const file = fileInput.files[0];

    if (!file) {
        return;
    }

    uploadDocument(file);

});


async function uploadDocument(file) {

    if (file.type !== "application/pdf") {

        alert("Please select a PDF file.");

        fileInput.value = "";

        return;
    }

    uploadButton.disabled = true;

    uploadButton.textContent = "Uploading...";

    const formData = new FormData();

    formData.append("file", file);

    let uploadedDocument = null;

    try {

        const response = await fetch(
            `${API_BASE_URL}/documents/upload/2`,
            {
                method: "POST",
                body: formData
            }
        );

        if (!response.ok) {

            const errorText = await response.text();

            console.error(
                "Upload failed:",
                response.status,
                errorText
            );

            throw new Error(
                `Upload failed (${response.status}): ${errorText}`
            );
        }

        uploadedDocument = await response.json();

        addDocumentToSidebar(
            uploadedDocument,
            "Uploaded"
        );

        uploadButton.textContent = "Processing...";

        await processDocument(
            uploadedDocument.id
        );

    } catch (error) {

        console.error(error);

        if (uploadedDocument) {

            updateDocumentStatus(
                uploadedDocument.id,
                "Processing failed"
            );
        }

        alert(error.message);

    } finally {

        uploadButton.disabled = false;

        uploadButton.textContent = "+ Upload";

        fileInput.value = "";
    }
}


/* =========================
   Process + Embed Document
   ========================= */

async function processDocument(documentId) {

    updateDocumentStatus(
        documentId,
        "Processing..."
    );

    try {

        const processResponse = await fetch(
            `${API_BASE_URL}/documents/process/${documentId}`,
            {
                method: "POST"
            }
        );

        if (!processResponse.ok) {

            const errorText = await processResponse.text();

            console.error(
                "Processing failed:",
                processResponse.status,
                errorText
            );

            throw new Error(
                `Processing failed (${processResponse.status}): ${errorText}`
            );
        }

        updateDocumentStatus(
            documentId,
            "Creating embeddings..."
        );

        const embeddingResponse = await fetch(
            `${API_BASE_URL}/documents/embeddings/${documentId}`,
            {
                method: "POST"
            }
        );

        if (!embeddingResponse.ok) {

            const errorText = await embeddingResponse.text();

            console.error(
                "Embedding failed:",
                embeddingResponse.status,
                errorText
            );

            throw new Error(
                `Embedding failed (${embeddingResponse.status}): ${errorText}`
            );
        }

        updateDocumentStatus(
            documentId,
            "Ready"
        );

    } catch (error) {

        console.error(error);

        updateDocumentStatus(
            documentId,
            "Processing failed"
        );

        throw error;
    }
}


/* =========================
   Document Sidebar
   ========================= */

function addDocumentToSidebar(doc, status) {

    const emptyMessage =
        documentList.querySelector(".empty-documents");

    if (emptyMessage) {
        emptyMessage.remove();
    }

    const documentItem = document.createElement("div");

    documentItem.className = "document-item";

    documentItem.dataset.documentId = doc.id;

    documentItem.innerHTML = `
        <div class="document-icon">
            📄
        </div>

        <div class="document-info">

            <div class="document-name">
                ${escapeHTML(doc.filename)}
            </div>

            <div class="document-status">
                ${status}
            </div>

        </div>

        <button
            class="delete-document-button"
            title="Delete document">
            ×
        </button>
    `;

    const deleteButton =
        documentItem.querySelector(
            ".delete-document-button"
        );

    deleteButton.addEventListener(
        "click",
        function () {

            deleteDocument(
                doc.id,
                documentItem
            );

        }
    );

    documentList.appendChild(documentItem);
}


function updateDocumentStatus(documentId, status) {

    const documentItem =
        documentList.querySelector(
            `[data-document-id="${documentId}"]`
        );

    if (!documentItem) {
        return;
    }

    const statusElement =
        documentItem.querySelector(".document-status");

    statusElement.textContent = status;
}


/* =========================
   Document Deletion
   ========================= */

async function deleteDocument(
    documentId,
    documentItem
) {

    const confirmed = confirm(
        "Are you sure you want to delete this document?"
    );

    if (!confirmed) {
        return;
    }

    const deleteButton =
        documentItem.querySelector(
            ".delete-document-button"
        );

    deleteButton.disabled = true;

    try {

        const response = await fetch(
            `${API_BASE_URL}/documents/${documentId}`,
            {
                method: "DELETE"
            }
        );

        if (!response.ok) {

            const errorText = await response.text();

            console.error(
                "Delete failed:",
                response.status,
                errorText
            );

            throw new Error(
                `Delete failed (${response.status}): ${errorText}`
            );
        }

        documentItem.remove();

        if (documentList.children.length === 0) {

            documentList.innerHTML = `
                <div class="empty-documents">
                    No documents uploaded
                </div>
            `;
        }

    } catch (error) {

        console.error(error);

        deleteButton.disabled = false;

        alert(
            "Failed to delete document. Please try again."
        );
    }
}


/* =========================
   Utility
   ========================= */

function escapeHTML(text) {

    const div = document.createElement("div");

    div.textContent = text;

    return div.innerHTML;
}

async function loadDocuments() {

    try {

        const response = await fetch(
            `${API_BASE_URL}/documents`
        );

        if (!response.ok) {
            throw new Error("Failed to load documents");
        }

        const documents = await response.json();

        documentList.innerHTML = "";

        if (documents.length === 0) {

            documentList.innerHTML = `
                <div class="empty-documents">
                    No documents uploaded
                </div>
            `;

            return;
        }

        for (const document of documents) {

            addDocumentToSidebar(
                document,
                document.status === "EMBEDDED"
                    ? "Ready"
                    : document.status
            );
        }

    } catch (error) {

        console.error(
            "Failed to load documents:",
            error
        );

    }
}