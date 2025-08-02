const express = require('express');
const cors = require('cors');
const app = express();

const admin = require('./firebase'); // tu configuración firebase-admin

// Middlewares
app.use(cors());
app.use(express.json());

app.post('/notificar', async (req, res) => {
  const { token, titulo, cuerpo } = req.body;

  if (!token || !titulo || !cuerpo) {
    return res.status(400).json({ error: 'Faltan datos' });
  }

  const message = {
    token: token,
    notification: {
      title: titulo,
      body: cuerpo
    }
  };

  try {
    const response = await admin.messaging().send(message);
    console.log('✅ Notificación enviada:', response);
    res.status(200).json({ success: true, response });
  } catch (error) {
    console.error('❌ Error al enviar notificación:', error);
    res.status(500).json({ error: error.message });
  }
});

app.listen(3000, () => {
  console.log('🚀 Servidor corriendo en http://localhost:3000');
});
