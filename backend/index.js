const express = require('express');
const admin = require('firebase-admin');
const cors = require('cors');

const app = express();
app.use(cors());
app.use(express.json());

// Carga las credenciales del archivo JSON aquí mismo
const serviceAccount = require('./service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
});

app.post('/notificar', async (req, res) => {
  const { token, title, body } = req.body;

  if (!token || !title || !body) {
    return res.status(400).json({ error: 'Faltan datos: token, title o body' });
  }

  const message = {
  data: {
    title: title || "Título por defecto",
    body: body || "Mensaje por defecto"
  },
  token: token,
  };
  try {
    const response = await admin.messaging().send(message);
    console.log('✅ Notificación enviada:', response);
    res.json({ success: true, response });
  } catch (error) {
    console.error('❌ Error al enviar la notificación:', error);
    res.status(500).json({ success: false, error: error.message });
  }
});
const tokens = [];
app.post('/guardar-token', (req, res) => {
  const { token } = req.body;
  if (!token) return res.status(400).json({ error: 'Falta token' });
  
  if (!tokens.includes(token)) {
    tokens.push(token);
    console.log('Token guardado:', token);
  }
  
  res.json({ success: true, tokens });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`🚀 Servidor corriendo en http://localhost:${PORT}`);
});
