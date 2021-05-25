import java.util.Objects;

class DocumentWithRelevance {
    private final String documentId;
    private final int relevance;

    DocumentWithRelevance(String documentId, int relevance) {
        this.documentId = documentId;
        this.relevance = relevance;
    }

    String getDocumentId() {
        return documentId;
    }

    int getRelevance() {
        return relevance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DocumentWithRelevance that = (DocumentWithRelevance) o;
        return that.relevance == relevance && Objects.equals(documentId, that.documentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(documentId, relevance);
    }
}