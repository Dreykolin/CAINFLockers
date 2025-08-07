const { GoogleAuth } = require('google-auth-library');
const admin = require('firebase-admin');
const express = require('express');
const cors = require('cors');
const fetch = require('node-fetch');

const app = express();
app.use(cors());
app.use(express.json());

// --- Configuración de Firebase Admin SDK ---
const serviceAccountPath = process.env.GOOGLE_APPLICATION_CREDENTIALS;
if (!serviceAccountPath) {
    console.error('Error: La variable de entorno GOOGLE_APPLICATION_CREDENTIALS no está definida.');
    process.exit(1);
}

try {
    const serviceAccount = require(serviceAccountPath);
    admin.initializeApp({
        credential: admin.credential.cert(serviceAccount),
    });
    console.log('Firebase Admin SDK inicializado correctamente.');
} catch (error) {
    console.error('Error al inicializar Firebase Admin SDK:', error.message);
    console.error('Asegúrate de que GOOGLE_APPLICATION_CREDENTIALS apunte a un archivo JSON válido.');
    process.exit(1);
}

const db = admin.firestore();

const SCOPES = ['https://www.googleapis.com/auth/firebase.messaging'];
const FCM_ENDPOINT = 'https://fcm.googleapis.com/v1/projects/cainflockers/messages:send';

const auth = new GoogleAuth({
    scopes: SCOPES,
});

async function getAccessToken() {
    const client = await auth.getClient();
    const accessTokenResponse = await client.getAccessToken();
    return accessTokenResponse.token;
}

// --- Endpoints ---

/**
 * Endpoint para guardar un token de notificación en Firestore.
 * Requiere: { token: string } en el cuerpo de la solicitud.
 */
app.post('/guardar_token', async (req, res) => {
    const { token } = req.body;
    if (!token) {
        return res.status(400).json({ error: 'Falta el token en el cuerpo de la solicitud.' });
    }

    try {
        await db.collection('fcmTokens').doc(token).set({
            token: token,
            timestamp: admin.firestore.FieldValue.serverTimestamp()
        });
        console.log('Token guardado en Firestore:', token);
        res.json({ success: true, message: 'Token guardado exitosamente.' });
    } catch (error) {
        console.error('Error al guardar el token en Firestore:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

/**
 * Endpoint para enviar una notificación FCM a un topic.
 * Requiere: { title: string, body: string } en el cuerpo de la solicitud.
 * Ya no necesita el 'token' de un dispositivo específico.
 */
app.post('/notificar_topic', async (req, res) => {
    const { title, body } = req.body;

    if (!title || !body) {
        return res.status(400).json({ error: 'Faltan datos: title o body en el cuerpo de la solicitud.' });
    }
    
    // Aquí definimos el topic al que queremos enviar la notificación
    const topic = 'new_requests'; 

    const message = {
        message: {
            topic: topic,
            notification: {
                title: title,
                body: body,
            },
            android: {
                priority: "high"
            }
        }
    };

    try {
        const accessToken = await getAccessToken();

        const response = await fetch(FCM_ENDPOINT, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${accessToken}`,
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(message),
        });

        if (!response.ok) {
            const errorData = await response.json();
            console.error('Error enviando mensaje FCM:', errorData);
            return res.status(response.status).json(errorData);
        }

        const data = await response.json();
        console.log(`Mensaje FCM enviado exitosamente al topic '${topic}':`, data);
        res.json({ success: true, data });
    } catch (error) {
        console.error('Error general al enviar notificación:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

/**
 * Endpoint para obtener todos los tokens guardados en Firestore.
 * Ya no es necesario para el nuevo enfoque de topics.
 * Se mantiene por si lo necesitas en el futuro.
 */
app.get('/tokens', async (req, res) => {
    if (req.query.key !== process.env.TOKENS_API_KEY) {
        return res.status(401).json({ error: 'No autorizado. La clave API proporcionada es incorrecta.' });
    }

    try {
        const snapshot = await db.collection('fcmTokens').get();
        const tokens = [];
        snapshot.forEach(doc => {
            tokens.push(doc.data().token);
        });
        console.log('Tokens recuperados de Firestore:', tokens.length);
        res.json({ tokens });
    } catch (error) {
        console.error('Error al obtener tokens de Firestore:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});


/**
 * Endpoint de salud simple para verificar que el servidor está funcionando.
 */
app.get("/ping", (req, res) => {
    res.status(200).send("OK");
});

// --- Inicio del Servidor ---
const PORT = process.env.PORT || 3000;
app.listen(PORT, '0.0.0.0', () => console.log(`Servidor escuchando en el puerto ${PORT}`));