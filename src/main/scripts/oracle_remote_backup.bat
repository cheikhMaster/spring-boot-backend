@echo off
setlocal enabledelayedexpansion

rem Script de sauvegarde à chaud Oracle pour environnement distant
rem Paramètres simplifiés
set DB_SID=%1
set BACKUP_TYPE=%2
set BACKUP_DIR=%3

echo ========================================
echo SAUVEGARDE ORACLE DISTANTE
echo ========================================
echo SID: %DB_SID%
echo Type: %BACKUP_TYPE%
echo Repertoire: %BACKUP_DIR%
echo Demarrage: %date% %time%
echo ========================================

rem Verification des parametres
if "%DB_SID%"=="" goto usage
if "%BACKUP_TYPE%"=="" goto usage
if "%BACKUP_DIR%"=="" goto usage

rem Normalisation du type de sauvegarde
if /i "%BACKUP_TYPE%"=="FULL" set BACKUP_TYPE=FULL
if /i "%BACKUP_TYPE%"=="INCREMENTAL" set BACKUP_TYPE=INCREMENTAL

rem Creation du repertoire si necessaire
if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"

rem Configuration de l'environnement Oracle
set ORACLE_SID=%DB_SID%
set ORACLE_HOME=C:\oracle\product\10.2.0\db_2
set PATH=%ORACLE_HOME%\bin;%PATH%

rem Fichier log
set LOG_FILE=%BACKUP_DIR%\backup.log
echo [%date% %time%] Demarrage sauvegarde %BACKUP_TYPE% pour %DB_SID% > "%LOG_FILE%"

rem Test de la connexion Oracle
echo Test connexion Oracle...
echo exit | sqlplus -s / as sysdba >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [%date% %time%] ERREUR: Connexion Oracle impossible >> "%LOG_FILE%"
    echo ERREUR: Connexion Oracle impossible
    exit /b 1
)

rem Creation du script RMAN
set RMAN_SCRIPT=%BACKUP_DIR%\rman.rcv
echo CONNECT TARGET / > "%RMAN_SCRIPT%"
echo CONFIGURE CONTROLFILE AUTOBACKUP ON; >> "%RMAN_SCRIPT%"
echo RUN { >> "%RMAN_SCRIPT%"
echo   ALLOCATE CHANNEL ch1 TYPE DISK; >> "%RMAN_SCRIPT%"

if /i "%BACKUP_TYPE%"=="FULL" (
    echo   BACKUP AS COMPRESSED BACKUPSET DATABASE FORMAT '%BACKUP_DIR%\db_%%d_%%T_%%s'; >> "%RMAN_SCRIPT%"
) else (
    echo   BACKUP AS COMPRESSED BACKUPSET INCREMENTAL LEVEL 1 DATABASE FORMAT '%BACKUP_DIR%\inc_%%d_%%T_%%s'; >> "%RMAN_SCRIPT%"
)

echo   BACKUP CURRENT CONTROLFILE FORMAT '%BACKUP_DIR%\ctrl_%%d_%%T_%%s.ctl'; >> "%RMAN_SCRIPT%"
echo   BACKUP SPFILE FORMAT '%BACKUP_DIR%\spfile_%%d_%%T_%%s.ora'; >> "%RMAN_SCRIPT%"
echo   RELEASE CHANNEL ch1; >> "%RMAN_SCRIPT%"
echo } >> "%RMAN_SCRIPT%"
echo EXIT; >> "%RMAN_SCRIPT%"

rem Execution de RMAN
echo Execution de la sauvegarde RMAN...
echo [%date% %time%] Execution RMAN >> "%LOG_FILE%"
"%ORACLE_HOME%\bin\rman" cmdfile="%RMAN_SCRIPT%" log="%LOG_FILE%" append

rem Verification du resultat
if %ERRORLEVEL% EQU 0 (
    echo [%date% %time%] Sauvegarde terminee avec succes >> "%LOG_FILE%"
    echo ========================================
    echo SAUVEGARDE TERMINEE AVEC SUCCES
    echo Heure de fin: %time%
    echo Consultez le log: %LOG_FILE%
    echo ========================================
    del "%RMAN_SCRIPT%" 2>nul
    exit /b 0
) else (
    echo [%date% %time%] ERREUR: Sauvegarde echouee >> "%LOG_FILE%"
    echo ========================================
    echo ERREUR: SAUVEGARDE ECHOUEE
    echo Code: %ERRORLEVEL%
    echo Consultez le log: %LOG_FILE%
    echo ========================================
    exit /b 1
)

:usage
echo ========================================
echo UTILISATION:
echo %0 SID TYPE REPERTOIRE
echo.
echo SID: Identifiant de la base Oracle
echo TYPE: FULL ou INCREMENTAL
echo REPERTOIRE: Chemin pour les fichiers
echo ========================================
exit /b 1