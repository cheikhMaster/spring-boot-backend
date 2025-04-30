#!/bin/bash
# oracle_hot_backup.sh - Script de sauvegarde à chaud Oracle

# Paramètres
DB_SID=$1         # SID de la base Oracle
BACKUP_TYPE=$2    # Type de sauvegarde (FULL, INCREMENTAL)
BACKUP_DIR=$3     # Répertoire de destination
DATE_FORMAT=$(date +%Y%m%d_%H%M%S)

# Vérifier les paramètres
if [ -z "$DB_SID" ] || [ -z "$BACKUP_TYPE" ] || [ -z "$BACKUP_DIR" ]; then
    echo "Utilisation: $0 <SID> <FULL|INCREMENTAL> <répertoire>"
    exit 1
fi

# Créer le répertoire de sauvegarde s'il n'existe pas
mkdir -p $BACKUP_DIR

# Fichier de log
LOG_FILE="${BACKUP_DIR}/backup_${DB_SID}_${DATE_FORMAT}.log"

# Fonction de journalisation
log() {
    echo "[$(date +"%Y-%m-%d %H:%M:%S")] $1" | tee -a $LOG_FILE
}

# Début de la sauvegarde
log "Démarrage de la sauvegarde $BACKUP_TYPE pour $DB_SID"

# Créer le script RMAN
RMAN_SCRIPT="${BACKUP_DIR}/rman_${DATE_FORMAT}.rcv"

if [ "$BACKUP_TYPE" = "FULL" ]; then
    cat > $RMAN_SCRIPT << EOF
CONNECT TARGET /
CONFIGURE CONTROLFILE AUTOBACKUP ON;
RUN {
  ALLOCATE CHANNEL ch1 TYPE DISK;
  BACKUP AS COMPRESSED BACKUPSET 
    DATABASE FORMAT '${BACKUP_DIR}/db_%d_%T_%s'
    PLUS ARCHIVELOG FORMAT '${BACKUP_DIR}/arch_%d_%T_%s';
  RELEASE CHANNEL ch1;
}
EXIT;
EOF
elif [ "$BACKUP_TYPE" = "INCREMENTAL" ]; then
    cat > $RMAN_SCRIPT << EOF
CONNECT TARGET /
CONFIGURE CONTROLFILE AUTOBACKUP ON;
RUN {
  ALLOCATE CHANNEL ch1 TYPE DISK;
  BACKUP AS COMPRESSED BACKUPSET 
    INCREMENTAL LEVEL 1 DATABASE FORMAT '${BACKUP_DIR}/inc1_%d_%T_%s'
    PLUS ARCHIVELOG FORMAT '${BACKUP_DIR}/arch_%d_%T_%s';
  RELEASE CHANNEL ch1;
}
EXIT;
EOF
else
    log "Type de sauvegarde non pris en charge: $BACKUP_TYPE"
    exit 1
fi

# Configuration de l'environnement Oracle
export ORACLE_SID=$DB_SID
export ORACLE_HOME=/u01/app/oracle/product/19.0.0/dbhome_1  # Adapter selon l'installation

# Exécuter RMAN
log "Exécution de RMAN avec le script $RMAN_SCRIPT"
$ORACLE_HOME/bin/rman cmdfile=$RMAN_SCRIPT log=$LOG_FILE

# Vérifier le résultat
if [ $? -eq 0 ]; then
    log "Sauvegarde terminée avec succès"
    
    # Calculer la taille de la sauvegarde
    BACKUP_SIZE=$(du -sh $BACKUP_DIR | awk '{print $1}')
    log "Taille de la sauvegarde: $BACKUP_SIZE"
    
    # Supprimer le script RMAN temporaire
    rm $RMAN_SCRIPT
    
    exit 0
else
    log "Erreur lors de la sauvegarde"
    exit 1
fi