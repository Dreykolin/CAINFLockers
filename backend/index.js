const { GoogleAuth } = require('google-auth-library');
const admin = require('firebase-admin');
const express = require('express');
const cors = require('cors');

const app = express();
app.use(cors());
app.use(express.json());
const fetch = require('node-fetch');

const serviceAccount = require(process.env.GOOGLE_APPLICATION_CREDENTIALS);
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
});

const SCOPES = ['https://www.googleapis.com/auth/firebase.messaging'];
const FCM_ENDPOINT = 'https://fcm.googleapis.com/v1/projects/cainflockers/messages:send'; // Cambia your-project-id

const auth = new GoogleAuth({
  scopes: SCOPES,
});

async function getAccessToken() {
  const client = await auth.getClient();
  const accessTokenResponse = await client.getAccessToken();
  return accessTokenResponse.token;
}
const tokens = [];
app.post('/guardar_token', (req, res) => {
  const { token } = req.body;
  if (!token) return res.status(400).json({ error: 'Falta token' });

  if (!tokens.includes(token)) {
    tokens.push(token);
    console.log('Token guardado:', token);
  }
  res.json({ success: true, tokens });
});



app.post('/notificar', async (req, res) => {
  const { token, title, body } = req.body;

  if (!token || !title || !body) {
    return res.status(400).json({ error: 'Faltan datos: token, title o body' });
  }

  const message = {
    message: {
      token: token,
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
      console.error('Error enviando mensaje:', errorData);
      return res.status(response.status).json(errorData);
    }

    const data = await response.json();
    console.log('Mensaje enviado:', data);
    res.json({ success: true, data });
  } catch (error) {
    console.error('Error al enviar notificación:', error);
    res.status(500).json({ success: false, error: error.message });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, '0.0.0.0', () => console.log(`Servidor escuchando en el puerto ${PORT}`));
