# Artículos en el mapa y flujo en el comercio

En Piku, cada **artículo publicado** es una fila en `piku_recompensas` (nombre, foto, tipo de beneficio, puntos para canjear).

## Comercio: publicar

1. Login **comercio** (ej. `cafe@martinez.com` / `comercio123` tras el seed).
2. Panel → **Publicar artículos**.
3. **+** → completar título, descripción, **foto** (galería), tipo (descuento %, 2x1, producto gratis), **puntos** para canjear, vigencia.
4. Guardar → el artículo queda activo y visible en el mapa para clientes.

## Cliente: mapa y catálogo

1. Login **cliente**.
2. Pestaña **Mapa** → pin del comercio (emoji + cantidad de artículos).
3. Tocar el pin → lista con **foto**, beneficio y puntos.
4. **Ver todos los artículos** → pantalla completa del comercio.
5. Tocar un artículo → detalle con foto grande, descuento/beneficio y botón **Canjear**.

## Cliente: envío a domicilio y más ofertas por teléfono

Si el comercio tiene **envíos** (`realiza_envios`) y **teléfono de contacto**:

- En el mapa (pin) o detalle del comercio/artículo aparece la tarjeta **Envío a domicilio**.
- **Pedir envío por WhatsApp** arma un mensaje con condiciones de envío y la dirección guardada en **Perfil** (si el cliente la completó).
- **Consultar más ofertas por WhatsApp** sirve para pedir artículos o descuentos **no publicados** en la app.

El comercio configura envíos y teléfono en: Panel → **Configurar envíos**.

## Cliente: visita al local

1. **Sumar puntos:** Inicio → **Escanear QR** del comercio (con GPS cerca del local).
2. **Canjear:** Mapa → comercio → artículo → **Canjear** (si alcanzan los puntos).
3. Mostrar el **código de canje** en caja; el comercio lo ve en **Historial de canjes** / notificaciones.

## API (referencia)

| Rol | Endpoint |
|-----|----------|
| Público | `GET /api/public/comercios`, `.../comercios/:id/ofertas`, `.../recompensas/:id` |
| Comercio | `POST /api/comercio/recompensas`, subida de imagen |
| Cliente | `POST /api/usuario/recompensas/:id/canjear`, `POST /api/qr/validar` |
