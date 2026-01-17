# TM WhatsApp Auto - Application Android

Application Android pour Teeshirt-Minute qui envoie automatiquement des messages WhatsApp Business (ou SMS) aux appelants en dehors des heures d'ouverture.

## Fonctionnalités

- 📞 Détection des appels entrants sur la SIM configurée
- 🔢 Filtrage automatique : uniquement les numéros mobiles français (06/07)
- 💬 Envoi automatique via WhatsApp Business
- 📨 Fallback SMS si le contact n'a pas WhatsApp
- 🌐 Récupération du message depuis WordPress (plugin TM PDV)
- 📊 Historique des messages envoyés
- ⚙️ Interface simple de configuration

## Prérequis

- Android 8.0 (API 26) minimum
- WhatsApp Business installé
- Plugin WordPress "Teeshirt Minute PDV" v2.1.0+ avec le module WhatsApp Auto activé

## Installation

1. Ouvrir le projet dans Android Studio
2. Synchroniser Gradle
3. Build > Generate Signed APK (ou Run pour le debug)

## Configuration

### Dans l'application :

1. Aller dans **Paramètres** (⚙️)
2. Entrer l'**URL WordPress** : `https://teeshirt-minute.com`
3. Entrer la **Clé API** (si configurée dans WordPress)
4. Sélectionner la **SIM à surveiller** (SIM 2 par défaut)
5. Tester la connexion
6. Enregistrer

### Permissions requises :

L'app demande automatiquement les permissions suivantes :
- **Téléphone** : Pour détecter les appels entrants
- **SMS** : Pour envoyer des SMS en fallback
- **Accessibilité** : Pour interagir avec WhatsApp Business

### Activer le service d'accessibilité :

1. L'app vous guidera vers les paramètres
2. Aller dans **Accessibilité** > **Services téléchargés**
3. Activer **TM WhatsApp Auto**
4. Confirmer l'activation

## Utilisation

1. **Activer le service** avec le switch sur l'écran principal
2. L'app tourne en arrière-plan (notification permanente)
3. Quand quelqu'un appelle en dehors des heures d'ouverture :
   - L'app vérifie le numéro (06/07 uniquement)
   - Contacte WordPress pour récupérer le message
   - Envoie via WhatsApp Business (ou SMS)
   - Affiche une notification de confirmation

## Structure du projet

```
app/src/main/java/com/teeshirtminute/whatsappauto/
├── TMWhatsAppAutoApp.kt          # Application class
├── data/
│   ├── ApiClient.kt              # Client API WordPress
│   ├── HistoryManager.kt         # Gestion de l'historique
│   ├── Models.kt                 # Classes de données
│   └── PreferencesManager.kt     # DataStore preferences
├── receivers/
│   ├── BootReceiver.kt           # Redémarrage auto
│   └── CallReceiver.kt           # Détection des appels
├── services/
│   ├── CallMonitorService.kt     # Service foreground
│   ├── MessageSenderService.kt   # Envoi des messages
│   └── WhatsAppAccessibilityService.kt  # Interaction WhatsApp
├── ui/
│   ├── HistoryActivity.kt        # Écran historique
│   ├── MainActivity.kt           # Écran principal
│   └── SettingsActivity.kt       # Écran paramètres
└── utils/
    ├── PermissionUtils.kt        # Gestion permissions
    └── PhoneUtils.kt             # Utilitaires numéros
```

## API WordPress

L'application communique avec le plugin TM PDV via l'API REST :

```
GET /wp-json/tm-pdv/v1/whatsapp-message

Headers (optionnel):
X-API-Key: votre-cle-api

Response:
{
  "should_send": true,
  "message": "Bonjour ! Notre boutique est fermée...",
  "message_sms": "Bonjour, boutique fermée...",
  "store_status": "Fermé - Réouverture mardi à 10h30"
}
```

## Notes techniques

- L'envoi WhatsApp utilise les Accessibility Services car l'API officielle WhatsApp Business nécessite un compte développeur Meta payant
- Le service tourne en foreground pour éviter d'être tué par Android
- Les préférences utilisent DataStore (pas SharedPreferences)

## Dépannage

**Le service ne détecte pas les appels :**
- Vérifier que les permissions téléphone sont accordées
- Vérifier que le service est bien activé (switch ON)

**WhatsApp ne s'ouvre pas :**
- Vérifier que WhatsApp Business est installé
- Vérifier que le service d'accessibilité est activé

**Le message n'est pas envoyé :**
- Vérifier la connexion internet
- Vérifier l'URL WordPress dans les paramètres
- Tester la connexion depuis l'app

## Licence

Propriétaire - Teeshirt-Minute.com
