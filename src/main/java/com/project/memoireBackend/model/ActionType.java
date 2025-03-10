package com.project.memoireBackend.model;

public enum ActionType {
    LOGIN("Connexion"),
    LOGOUT("Déconnexion"),
    CREATE_USER("Création utilisateur"),
    MODIFY_USER("Modification utilisateur"),
    DELETE_USER("Suppression utilisateur"),
    ADD_DATABASE("Ajout base de données"),
    MODIFY_DATABASE("Modification base de données"),
    DELETE_DATABASE("Suppression base de données"),
    START_BACKUP("Début sauvegarde"),
    COMPLETE_BACKUP("Fin sauvegarde"),
    FAILED_BACKUP("Échec sauvegarde"),
    CONFIGURE_SYSTEM("Configuration système"),
    SYSTEM_ERROR("Erreur système");

    private final String displayName;

    ActionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}