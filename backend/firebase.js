// firebase.js
const admin = require('firebase-admin');
const serviceAccount = require('./firebase-key.json'); // 🔒 No subir a GitHub

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

module.exports = admin;