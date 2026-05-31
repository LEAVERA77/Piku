# Icono y splash según versión de Android

## Por qué no iOS ni por marca de celular

Este proyecto es **solo Android**. El icono del escritorio y la splash del sistema no se configuran con recursos de iOS.

Android **no permite** elegir drawables distintos por Samsung o Motorola. Se usa **nivel de API** (`v34`, etc.).

## Qué hace Piku

| Recurso | Dispositivos | Tamaño del logo en el lienzo 108dp |
|---------|----------------|-------------------------------------|
| `drawable-nodpi/piku_logo_icon_adaptive.png` | API 33 y menor (p. ej. Motorola G30 en Android 11–13) | ~18%, centrado |
| `drawable-nodpi-v34/piku_logo_icon_adaptive.png` | API 34+ / Android 14+ (p. ej. Samsung A16) | ~32%, centrado |

El umbral **v34** evita que un Motorola actualizado a Android 12 (API 31) use el PNG grande pensado para Samsung.

`ic_launcher_foreground_piku.xml` y `Theme.Piku.Splash` apuntan a `@drawable/piku_logo_icon_adaptive`.

## Ajustar tamaños

Regenerar los PNG cambiando el porcentaje del lienzo (18 y 32 en el script de generación).

Tras cambiar: desinstalar la app e instalar de nuevo; borrar caché del launcher si el icono no actualiza.
