package com.harvanna.wallet;

/**
 * Sketchware-compatible application configuration.
 */
public final class AjooConfig {
    private AjooConfig() {}

    public static final String FIREBASE_API_KEY = "AIzaSyBD5UbBOBYlsNMYwL84h1ZP7nlW39YPMXA";
    public static final String PROJECT_ID = "capital-oracle-project";
    
    public static final String BASE_FIRESTORE_URL = 
        "https://firestore.googleapis.com/v1/projects/" + PROJECT_ID + "/databases/(default)/documents/";

    public static String getDocumentUrl(String collection, String documentId) {
        return BASE_FIRESTORE_URL + collection + "/" + documentId + "?key=" + FIREBASE_API_KEY;
    }

    public static String getCollectionUrl(String collection) {
        return BASE_FIRESTORE_URL + collection + "?key=" + FIREBASE_API_KEY;
    }

    public static String getQueryUrl() {
        return "https://firestore.googleapis.com/v1/projects/" + PROJECT_ID + "/databases/(default)/documents:runQuery?key=" + FIREBASE_API_KEY;
    }
}
