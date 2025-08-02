const express = require('express');
const cors = require('cors');
const axios = require('axios');
const webpush = require('web-push');
const bodyParser = require('body-parser');
const sqlite3 = require('sqlite3').verbose();
const app = express();
const db = new sqlite3.Database('./base_de_datos.db');  // Acá creamos nuestra base de datos
const Tables = require('./models/tables');
const apiKey_Weather = '8c602967b810f8b7f537a67aaeaef81d';
const argon2 = require('argon2'); // ⬅️ Importamos argon2
Tables(db);
//XD

// Middleware
app.use(cors());
app.use(express.json());
app.use(bodyParser.json());

// Web Push setup
const publicVapidKey = "BPaW76e491m4ebBJFa2s5aswfKU6jhSoVFDsAV_z0cbFFe5uGinqGn9PC15ZG7iQ6kopIZ3u4rnUCRqrkcXm3wc";
const privateVapidKey = "-HOhVaQD_VkcLgdT3GHGd6luMPP0RdBfbLZ6aCMl80s";
webpush.setVapidDetails('mailto:lcao2018@udec.cl', publicVapidKey, privateVapidKey);

let subscriptions = []; //Curioso digamos







// Endpoints







// Este endpoint devuelve las últimas 3 actividades de un usuario.
app.get('/api/historial-reciente/:usuario_id', (req, res) => {
    const { usuario_id } = req.params;

    if (!usuario_id) {
        return res.status(400).json({ error: 'Falta el ID del usuario.' });
    }

    // Pide el historial, lo ordena por fecha descendente y se queda solo con los 3 primeros.
    const sql = `
        SELECT ha.fecha, a.nombre
        FROM historial_actividades ha
        JOIN actividades a ON ha.actividad_id = a.id
        WHERE ha.usuario_id = ?
        ORDER BY ha.fecha DESC
        LIMIT 3
    `;

    db.all(sql, [usuario_id], (err, rows) => {
        if (err) {
            console.error('Error al obtener historial reciente:', err.message);
            return res.status(500).json({ error: 'Error en la base de datos.' });
        }
        res.json(rows);
    });
});






app.post('/guardar_actividad', (req, res) => {
  const { 
    nombre, 
    descripcion, 
    temperatura, 
    viento, 
    lluvia, 
    uv, 
    outdoor, 
    indoor, 
    intellectual, 
    sports, 
    usuario_id 
  } = req.body;

  // Validación básica
  if (!nombre || !descripcion) {
    return res.status(400).json({
      success: false,
      error: 'Nombre y descripción son obligatorios'
    });
  }

  const sql = `
    INSERT INTO actividades 
    (nombre, descripcion, temperatura, viento, lluvia, uv, outdoor, indoor, intellectual, sports, usuario_id) 
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  `;

  const params = [
    nombre,
    descripcion,
    temperatura || null,
    viento || null,
    lluvia || null,
    uv || null,
    outdoor || 0,
    indoor || 0,
    intellectual || 0,
    sports || 0,
    usuario_id || null
  ];

  db.run(sql, params, function(err) {
    if (err) {
      console.error('Error al guardar actividad:', err);
      return res.status(500).json({ 
        success: false,
        error: 'Error en la base de datos' 
      });
    }
    
    res.status(201).json({
      success: true,
      message: 'Actividad guardada correctamente',
      id: this.lastID,
      data: {
        nombre,
        descripcion,
        temperatura,
        viento,
        lluvia,
        uv,
        outdoor,
        indoor,
        intellectual,
        sports,
        usuario_id
      }
    });
  });
});

// Endpoint para obtener usuarios por mes de creación
app.get('/usuarios_por_mes', (req, res) => {
  const añoActual = new Date().getFullYear();

  const sql = `
    SELECT 
      strftime('%m', fecha_registro) AS mes,
      COUNT(*) AS cantidad
    FROM usuarios
    WHERE strftime('%Y', fecha_registro) = ?
    GROUP BY mes
    ORDER BY mes
  `;

  db.all(sql, [añoActual.toString()], (err, rows) => {
    if (err) {
      console.error('Error al obtener usuarios por mes:', err);
      return res.status(500).json({ error: 'Error al obtener datos' });
    }

    const meses = [
      'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
      'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'
    ];

    const data = Array.from({ length: 12 }, (_, i) => {
      const mesStr = String(i + 1).padStart(2, '0');
      const fila = rows.find(r => r.mes === mesStr);
      return {
        mes: meses[i],
        cantidad: fila ? fila.cantidad : 0
      };
    });

    res.json(data);
  });
});

//Dashboard
app.get('/admin/dashboard', (req, res) => {
  const dashboard = {
    total_usuarios: 0,
    total_actividades: 0,
    actividades_recomendadas: 0,
    ubicaciones: 0,
    historial: 0
  };

  const queries = {
    total_usuarios: 'SELECT COUNT(*) AS count FROM usuarios',
    total_actividades: 'SELECT COUNT(*) AS count FROM actividades',
    actividades_recomendadas: 'SELECT COUNT(*) AS count FROM actividades WHERE usuario_id IS NULL',
    historial: 'SELECT COUNT(*) AS count FROM historial_actividades'
  };

  let completadas = 0;

  for (const key in queries) {
    db.get(queries[key], [], (err, row) => {
      if (err) {
        console.error(`Error al obtener ${key}:`, err.message);
        return res.status(500).json({ error: `Error al obtener ${key}` });
      }

      dashboard[key] = row.count;
      completadas++;

      if (completadas === Object.keys(queries).length) {
        res.json(dashboard);
      }
    });
  }
});



app.post('/guardar_usuario', async (req, res) => {
    const { email, contraseña } = req.body;

    if (!email || !contraseña) {
        return res.status(400).send('Email y contraseña son obligatorios');
    }

    try {
        // ⬅️ Hasheamos la contraseña antes de guardarla
        const hashedPassword = await argon2.hash(contraseña);

        const sql = 'INSERT INTO usuarios (email, contraseña) VALUES (?, ?)';
        db.run(sql, [email, hashedPassword], function (err) {
            if (err) {
                console.error(err);
                return res.status(500).send('Error al guardar el usuario');
            }
            res.send('Usuario guardado correctamente');
        });
    } catch (err) {
        console.error('Error al hashear la contraseña:', err);
        res.status(500).send('Error al procesar la contraseña');
    }
});

//Actualmente tenemos una tabla de ubicaciones eb la bdd, tocará ver si realmente es útil, pero el tema es que se guardan co esto.
app.post('/guardar_ubicacion', (req, res) => {
  const { latitud, longitud, ciudad } = req.body;
  const sql = 'INSERT INTO ubicaciones (latitud, longitud, ciudad) VALUES (?, ?, ?)';

  db.run(sql, [latitud, longitud, ciudad], function(err) {
    if (err) {
      return res.status(500).json({ error: 'Error al guardar la ubicación' });
    }
    res.send(`Ubicación guardada: ${ciudad}, Lat: ${latitud}, Lon: ${longitud}`);
  });
});


// Obtener todas las recomendaciones genéricas (usuario_id es NULL)
app.get('/recomendaciones', (req, res) => {
  const sql = `SELECT * FROM actividades WHERE usuario_id IS NULL`;

  db.all(sql, [], (err, rows) => {
    if (err) {
      console.error('Error al obtener recomendaciones:', err);
      return res.status(500).json({ error: 'Error al obtener recomendaciones' });
    }
    res.json(rows);
  });
});


//El más importante. Permite que el usuario guarde sus actividades.
//también está programado para que si se añade una actividad sin user id, este esté disponible para todos (como modo predeterminado)
app.post('/guardar_recomendacion', async (req, res) => {
  try {
    const { nombre, descripcion, temperatura, viento, lluvia, uv, outdoor, indoor, sports, intellectual } = req.body;

    // Validación básica
    if (!nombre || !descripcion) {
      return res.status(400).json({ 
        success: false,
        error: 'Nombre y descripción son obligatorios' 
      });
    }

    // Insertar en SQLite
    const sql = `
      INSERT INTO actividades 
      (nombre, descripcion, temperatura, viento, lluvia, uv, outdoor, indoor, sports, intellectual, usuario_id)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    `;
    
    db.run(sql, [
      nombre, descripcion, 
      temperatura || null, 
      viento || null, 
      lluvia || null, 
      uv || null,
      outdoor || 3,
      indoor || 3,
      sports || 3,
      intellectual || 3,
      null // usuario_id null para recomendaciones globales
    ], function(err) {
      if (err) {
        console.error('Error en la base de datos:', err);
        return res.status(500).json({ 
          success: false,
          error: 'Error al guardar en la base de datos' 
        });
      }

      // Éxito - devolver los datos guardados
      res.status(201).json({
        success: true,
        actividad: {
          id: this.lastID,
          nombre,
          descripcion,
          temperatura,
          viento,
          lluvia,
          uv,
          outdoor,
          indoor,
          sports,
          intellectual
        }
      });
    });

  } catch (err) {
    console.error('Error inesperado:', err);
    res.status(500).json({ 
      success: false,
      error: 'Error interno del servidor' 
    });
  }
});

app.get('/usuarios', (req, res) => {
  const sql = 'SELECT id, email FROM usuarios';
  db.all(sql, [], (err, rows) => {
    if (err) {
      console.error('Error al obtener usuarios:', err);
      return res.status(500).json({ error: 'Error al obtener los usuarios' });
    }
    res.json(rows);
  });
});

// PUT para editar recomendación
app.put('/editar_recomendacion/:id', (req, res) => {
  const { id } = req.params;
  const { nombre, descripcion, temperatura, viento, lluvia, uv, outdoor, indoor, intellectual, sports } = req.body;

  if (!nombre || !descripcion) {
    return res.status(400).json({ 
      success: false,
      error: 'Nombre y descripción son obligatorios' 
    });
  }

  const sql = `
    UPDATE actividades SET 
      nombre = ?, 
      descripcion = ?, 
      temperatura = ?, 
      viento = ?, 
      lluvia = ?, 
      uv = ?,
      outdoor = ?,
      indoor = ?,
      intellectual = ?,
      sports = ?
    WHERE id = ? AND usuario_id IS NULL
  `;

  const params = [
    nombre,
    descripcion,
    temperatura || null,
    viento || null,
    lluvia || null,
    uv || null,
    outdoor || 0,
    indoor || 0,
    intellectual || 0,
    sports || 0,
    id
  ];

  db.run(sql, params, function(err) {
    if (err) {
      console.error("Error al editar:", err);
      return res.status(500).json({ 
        success: false,
        error: 'Error en la base de datos' 
      });
    }
    
    if (this.changes === 0) {
      return res.status(404).json({
        success: false,
        error: 'No se encontró la recomendación o no es editable'
      });
    }
    
    res.json({
      success: true,
      message: 'Recomendación actualizada exitosamente',
      id: id,
      changes: this.changes
    });
  });
});

// DELETE para eliminar recomendación
app.delete('/eliminar_recomendacion/:id', (req, res) => {
  const { id } = req.params;

  const sql = 'DELETE FROM actividades WHERE id = ? AND usuario_id IS NULL';

  db.run(sql, [id], function(err) {
    if (err) {
      console.error("Error al eliminar:", err);
      return res.status(500).json({ 
        success: false,
        error: 'Error en la base de datos' 
      });
    }
    
    if (this.changes === 0) {
      return res.status(404).json({
        success: false,
        error: 'No se encontró la recomendación o no es eliminable'
      });
    }
    
    res.json({
      success: true,
      message: 'Recomendación eliminada exitosamente',
      id: id
    });
  });
});

//endopoints gustos

// Obtener actividades de un usuario específico
app.get('/actividades/usuario/:usuario_id', (req, res) => {
  const { usuario_id } = req.params;
  
  const sql = `SELECT * FROM actividades WHERE usuario_id = ?`;
  
  db.all(sql, [usuario_id], (err, rows) => {
    if (err) {
      console.error('Error al obtener actividades:', err);
      return res.status(500).json({ 
        success: false,
        error: 'Error al obtener actividades' 
      });
    }
    res.json({
      success: true,
      data: rows
    });
  });
});


// Actualizar preferencias de usuario
app.put('/usuarios/preferencias/:usuario_id', (req, res) => {
  const { usuario_id } = req.params;
  const { outdoor, indoor, sports, intellectual } = req.body;

  const sql = `
    UPDATE usuarios SET
      outdoor = ?,
      indoor = ?,
      sports = ?,
      intellectual = ?
    WHERE id = ?
  `;

  const params = [
    outdoor || 3,
    indoor || 3,
    sports || 3,
    intellectual || 3,
    usuario_id
  ];

  db.run(sql, params, function(err) {
    if (err) {
      console.error('Error al actualizar preferencias:', err);
      return res.status(500).json({
        success: false,
        error: 'Error al actualizar preferencias'
      });
    }
    
    res.json({
      success: true,
      message: 'Preferencias actualizadas correctamente',
      changes: this.changes
    });
  });
});

// Obtener preferencias de usuario
app.get('/usuarios/preferencias/:usuario_id', (req, res) => {
  const { usuario_id } = req.params;

  const sql = `
    SELECT outdoor, indoor, sports, intellectual 
    FROM usuarios 
    WHERE id = ?
  `;

  db.get(sql, [usuario_id], (err, row) => {
    if (err) {
      console.error('Error al obtener preferencias:', err);
      return res.status(500).json({
        success: false,
        error: 'Error al obtener preferencias'
      });
    }
    
    if (!row) {
      return res.status(404).json({
        success: false,
        error: 'Usuario no encontrado'
      });
    }
    
    res.json({
      success: true,
      data: row
    });
  });
});












//Esto no guarda en una base de datos, esto solo captura el clima en base a la latencia y longitud, usada para 
app.post('/clima', async (req, res) => {
    const { lat, lon } = req.body;
    const url = `https://api.openweathermap.org/data/2.5/weather?lat=${lat}&lon=${lon}&appid=${apiKey_Weather}&units=metric&lang=es`;

    try {
        const { data } = await axios.get(url);
        res.json({
            temperatura: data.main.temp,
            humedad: data.main.humidity,
            viento: data.wind.speed,
            tiempo_id: data.weather[0].id,
            descripcion: data.weather[0].description,
            ciudad: data.name,
            icon: data.weather[0].icon // ⬅️ ¡AQUÍ ESTÁ LA LÍNEA CLAVE!
        });
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Error al obtener el clima' });
    }
});



//Si quieres añadir una ciudad, se usa este, que hace lo mismo que el anterior, pero en base al nombre de la ciudad
app.post('/clima_por_ciudad', async (req, res) => {
  const { ciudad } = req.body;
    const url = `https://api.openweathermap.org/data/2.5/weather?q=${encodeURIComponent(ciudad)}&appid=${apiKey_Weather}&units=metric&lang=es`;

  try {
    const { data } = await axios.get(url);
    res.json({
      temperatura: data.main.temp,
      humedad: data.main.humidity,
      viento: data.wind.speed,
      tiempo_id: data.weather[0].id,
      descripcion: data.weather[0].description,
      ciudad: data.name
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Error al obtener el clima para esa ciudad' });
  }
});



app.post('/pronostico', async (req, res) => {
    const { lat, lon } = req.body;
    const url = `https://api.openweathermap.org/data/3.0/onecall?lat=${lat}&lon=${lon}&appid=${apiKey_Weather}&units=metric&lang=es`;

    try {
        const { data } = await axios.get(url);

        const resumenDiario = data.daily.slice(0, 4).map(day => {
    
            const fecha = new Date(day.dt * 1000).toLocaleDateString('es-ES');
            const maxTemp = day.temp.max;
            const minTemp = day.temp.min;
            const descripcion = day.weather[0].description;
            const icono = day.weather[0].icon;

            return {
                fecha,
                temp_max: maxTemp.toFixed(1),
                temp_min: minTemp.toFixed(1),
                descripcion,
                icono,
            };
        });

        res.json({
            ciudad: data.timezone,
            resumenDiario 
        });

    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Error al obtener el pronóstico' });
    }
});





app.post('/login', (req, res) => {
    const { email, password } = req.body;

    // 1. Obtenemos el usuario por email (sin comparar la contraseña aún)
    const query = `
        SELECT id, email, contraseña, tours_vistos, outdoor, indoor, sports, intellectual 
        FROM usuarios 
        WHERE email = ?
    `;

    db.get(query, [email], async (err, row) => {
        if (err) {
            console.error('Error en la consulta de login:', err.message);
            return res.status(500).json({ success: false, message: 'Error en la base de datos' });
        }

        if (!row) {
            return res.status(401).json({ success: false, message: 'Correo o contraseña incorrectos' });
        }

        try {
            // 2. Verificamos la contraseña con argon2
            const isValid = await argon2.verify(row.contraseña, password);

            if (!isValid) {
                return res.status(401).json({ success: false, message: 'Correo o contraseña incorrectos' });
            }

            // 3. Parseamos tours_vistos y devolvemos datos del usuario
            const toursVistosObject = JSON.parse(row.tours_vistos);

            res.json({
                success: true,
                message: 'Inicio de sesión exitoso',
                token: 'este-es-un-token-de-ejemplo',
                usuario_id: row.id,
                email: row.email,
                tours_vistos: toursVistosObject,
                preferencias: {
                    outdoor: row.outdoor,
                    indoor: row.indoor,
                    sports: row.sports,
                    intellectual: row.intellectual
                }
            });

        } catch (err) {
            console.error('Error al verificar contraseña con argon2:', err.message);
            return res.status(500).json({ success: false, message: 'Error al verificar contraseña' });
        }
    });
});


// Este endpoint ahora es más inteligente: actualiza un tour específico por su nombre.
app.post('/tour-completado', (req, res) => {
    // 1. Ahora esperamos recibir el nombre del tour que se completó (ej: "home", "historial")
    const { usuario_id, tour_name } = req.body;

    if (!usuario_id || !tour_name) {
        return res.status(400).json({ error: 'Faltan datos: usuario_id y tour_name son obligatorios.' });
    }

    // 2. Primero, obtenemos el estado actual de los tours del usuario.
    const getSql = `SELECT tours_vistos FROM usuarios WHERE id = ?`;

    db.get(getSql, [usuario_id], (err, row) => {
        if (err) {
            console.error('Error al obtener los tours:', err.message);
            return res.status(500).json({ error: 'Error en la base de datos.' });
        }
        if (!row) {
            return res.status(404).json({ message: 'Usuario no encontrado.' });
        }

        // 3. Convertimos el string de la BD a un objeto, actualizamos el tour específico a 'true'.
        const toursVistosObject = JSON.parse(row.tours_vistos);
        toursVistosObject[tour_name] = true;

        // 4. Convertimos el objeto modificado de nuevo a un string para guardarlo en la BD.
        const updatedToursVistosString = JSON.stringify(toursVistosObject);

        // 5. Guardamos el string actualizado en la base de datos.
        const updateSql = `UPDATE usuarios SET tours_vistos = ? WHERE id = ?`;
        db.run(updateSql, [updatedToursVistosString, usuario_id], function (updateErr) {
            if (updateErr) {
                console.error('Error al actualizar el tour:', updateErr.message);
                return res.status(500).json({ error: 'Error al marcar el tour como completado.' });
            }
            res.json({ success: true, message: `Tour '${tour_name}' marcado como completado.` });
        });
    });
});











// No tengo idea de este, peeeero es correcto que el backend maneje las recomendaciones y no el front
// RE: estas actividades son las recomendadas por desarrolladores. lo ideal sería entregarselas a la db, pero por ahora están acá.
app.post('/recomendar', (req, res) => {
  const { temperatura, viento, tiempo_id, preferencias } = req.body;

  const actividades = [
    {nombre: "Ciclismo", descripcion: "Salir en bicicleta al parque", temperatura: 22, viento: 10, lluvia: 0, uv: 1, outdoor: 4, indoor: 1, intellectual: 1, sports: 3},
    {nombre: "Picnic", descripcion: "Picnic con amigos", temperatura: 25, viento: 8, lluvia: 0, uv: 1, outdoor: 3, indoor: 1, intellectual: 1, sports: 1},
    {nombre: "Senderismo", descripcion: "Subida en el cerro", temperatura: 18, viento: 12, lluvia: 0, uv: 1, outdoor: 4, indoor: 1, intellectual: 1, sports: 3},
    {nombre: "Lectura en balcón", descripcion: "Leer en el balcón", temperatura: 20, viento: 5, lluvia: 0, uv: 7, outdoor: 2, indoor: 5, intellectual: 5, sports: 1},
    {nombre: "Lectura junto al fuego", descripcion: "Leer en la casa", temperatura: 10, viento: 7, lluvia: 0, uv: 7, outdoor: 1, indoor: 4, intellectual: 5, sports: 1},
    {nombre: "Fútbol", descripcion: "Jugar fútbol con amigos", temperatura: 24, viento: 9, lluvia: 0, uv: 1, outdoor: 3, indoor: 1, intellectual: 1, sports: 4},

    /*
    { nombre: 'Leer un libro', categoria: 'relajación', exterior: false, lluvia: true },
    { nombre: 'Correr en el parque', categoria: 'ejercicio', exterior: true, lluvia: false },
    { nombre: 'Ver una película', categoria: 'ocio', exterior: false, lluvia: true },
    { nombre: 'Andar en bicicleta', categoria: 'ejercicio', exterior: true, lluvia: false },
    { nombre: 'Meditar', categoria: 'relajación', exterior: false, lluvia: true }
     */
  ];

  const llueve = tiempo_id >= 200 && tiempo_id < 700;
  const vientoFuerte = viento > 10;


  const filteredActivities = actividades.filter(activity => {
    return (
      activity.outdoor <= preferencias.outdoor &&
      activity.indoor <= preferencias.indoor &&
      activity.intellectual <= preferencias.intellectual &&
      activity.sports <= preferencias.sports &&
      temperatura >= activity.temperatura - 10 &&
      temperatura <= activity.temperatura + 10 &&
      viento <= activity.viento + 10

      
    );
  });
  res.json({ actividades: filteredActivities });
});







//Usado por las notificaciones en 2do plano
app.post('/subscribe', (req, res) => {
  subscriptions.push(req.body);
  console.log('📬 Nueva suscripción push');
  res.status(201).json({ message: 'Subscribed!' });
});

//Otro que no tengo idea, seguro es de la linna porque es lo que lanzaba la notificación
// ⏰ Envío de notificaciones periódicas
setInterval(() => {
  const message = {
    title: 'Clima de hoy en ubicación',
    body: 'temperatura: ,actividad recomenddo: salir a caminar.'
  };
  sendToAll(message);
}, 30000);

async function sendToAll(message) {
  const payload = JSON.stringify(message);
  await Promise.allSettled(
    subscriptions.map(sub =>
      webpush.sendNotification(sub, payload).catch(err => {
        console.error('Error al enviar push:', err);
      })
    )
  );
}



//El modo Admin, esto NO es posible según yo, usuario id es una clave primaria
app.get('/admin', (req, res) => {
  const query = `SELECT * FROM actividades WHERE usuario_id IS NULL`;
  db.all(query, [], (err, rows) => {
    if (err) {
      console.error('Error al obtener actividades del admin:', err.message);
      return res.status(500).json({ error: 'Error en la base de datos' });
    }
    res.json(rows);
  });
});


//Esto es lo que permite obtener las actividades del usuario que anteriormente fueron registradas en la base de datos,.
app.get('/actividades/:usuario_id', (req, res) => {
  const { usuario_id } = req.params;
  const query = `SELECT * FROM actividades WHERE usuario_id = ?`;

  db.all(query, [usuario_id], (err, rows) => {
    if (err) {
      console.error('Error al obtener actividades:', err.message);
      return res.status(500).json({ error: 'Error en la base de datos' });
    }
    res.json(rows);
  });
});



app.post('/registrar_actividad', (req, res) => {
  const { usuario_id, actividad_id } = req.body;

  if (!usuario_id || !actividad_id) {
    return res.status(400).json({ error: 'Faltan datos: usuario_id y actividad_id son obligatorios' });
  }

  const sql = `
    INSERT INTO historial_actividades (usuario_id, actividad_id)
    VALUES (?, ?)
  `;

  db.run(sql, [usuario_id, actividad_id], function(err) {
    if (err) {
      console.error('Error al guardar en historial:', err.message);
      return res.status(500).json({ error: 'Error al registrar actividad' });
    }
    res.json({ success: true, message: 'Actividad registrada en historial' });
  });
});

app.get('/historial/:usuario_id', (req, res) => {
  const { usuario_id } = req.params;
  const { mes, año } = req.query;

  let sql = `
    SELECT ha.fecha, a.nombre, a.descripcion
    FROM historial_actividades ha
    JOIN actividades a ON ha.actividad_id = a.id
    WHERE ha.usuario_id = ?
  `;
  const params = [usuario_id];

  if (mes && año) {
    sql += ` AND strftime('%m', ha.fecha) = ? AND strftime('%Y', ha.fecha) = ?`;
    params.push(mes.padStart(2, '0'), año);
  }

  sql += ` ORDER BY ha.fecha DESC`;

  db.all(sql, params, (err, rows) => {
    if (err) {
      console.error('Error al obtener historial:', err.message);
      return res.status(500).json({ error: 'Error al obtener historial' });
    }
    res.json(rows);
  });
});

app.post('/api/guardar-preferencia', (req, res) => {
    const { usuario_id, categoria, valor } = req.body;

    // Validaciones básicas
    if (!usuario_id || !categoria || typeof valor !== 'number') {
        return res.status(400).json({ error: 'Datos incompletos o inválidos' });
    }
    const categoriasValidas = ['outdoor', 'indoor', 'intellectual', 'sports'];
    if (!categoriasValidas.includes(categoria)) {
        return res.status(400).json({ error: 'Categoría inválida' });
    }

    // La consulta ahora es un simple UPDATE en la tabla de usuarios.
    // No necesitamos verificar si existe primero, porque la fila del usuario
    // se crea al registrarse, y los gustos tienen valores por defecto.
    const query = `UPDATE usuarios SET ${categoria} = ? WHERE id = ?`;

    db.run(query, [valor, usuario_id], function (err) {
        if (err) {
            console.error("Error actualizando gustos en tabla usuarios:", err.message);
            return res.status(500).json({ error: 'Error al actualizar la base de datos.' });
        }
        if (this.changes === 0) {
            return res.status(404).json({ error: 'Usuario no encontrado.' });
        }
        res.json({ message: 'Preferencia actualizada correctamente.' });
    });
});
// 2. Endpoint para OBTENER las preferencias (también más simple)
app.get('/api/obtener-preferencias/:usuario_id', (req, res) => {
    const { usuario_id } = req.params;

    // La consulta ahora busca directamente en la tabla de usuarios.
    const sql = `SELECT outdoor, indoor, sports, intellectual FROM usuarios WHERE id = ?`;

    db.get(sql, [usuario_id], (err, row) => {
        if (err) {
            console.error('Error al obtener preferencias de la tabla usuarios:', err.message);
            return res.status(500).json({ error: 'Error en la base de datos.' });
        }
        res.json(row || { outdoor: 3, indoor: 3, sports: 3, intellectual: 3 });
    });
});

// Este endpoint marca el tour de preferencias como completado.
app.post('/preferencias-completadas', (req, res) => {
    const { usuario_id } = req.body;

    if (!usuario_id) {
        return res.status(400).json({ error: 'Falta el ID del usuario.' });
    }

    // 1. Obtenemos el estado actual de los tours del usuario.
    const getSql = `SELECT tours_vistos FROM usuarios WHERE id = ?`;

    db.get(getSql, [usuario_id], (err, row) => {
        if (err) {
            console.error("Error obteniendo tours para actualizar preferencias:", err.message);
            return res.status(500).json({ error: 'Error en la base de datos.' });
        }
        if (!row) {
            return res.status(404).json({ message: 'Usuario no encontrado.' });
        }

        // 2. Actualizamos el flag específico a 'true'.
        const toursVistos = JSON.parse(row.tours_vistos);
        toursVistos.preferencias_configuradas = true;
        const updatedTours = JSON.stringify(toursVistos);

        // 3. Guardamos el nuevo estado en la base de datos.
        const updateSql = `UPDATE usuarios SET tours_vistos = ? WHERE id = ?`;
        db.run(updateSql, [updatedTours, usuario_id], (updateErr) => {
            if (updateErr) {
                console.error("Error actualizando flag de preferencias:", updateErr.message);
                return res.status(500).json({ error: 'Error al actualizar la base de datos.' });
            }
            res.json({ success: true, message: 'Preferencias marcadas como completadas.' });
        });
    });
});






//Solo es un flag
app.listen(3000, () => {
  console.log('🚀 Servidor corriendo en http://localhost:3000');
});

