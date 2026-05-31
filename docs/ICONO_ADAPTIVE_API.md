# Icono y splash según versión de Android

## Por qué no iOS ni por marca de celular

Este proyecto es **solo Android**. El icono del escritorio y la splash del sistema no se configuran con recursos de iOS.

Android **no permite** elegir drawables distintos por Samsung o Motorola. Lo habitual es usar **nivel de API** (`v31`, `v26`, etc.), que suele alinearse con cómo cada launcher enmascara el icono.

## Qué hace Piku

| Recurso | Dispositivos | Tamaño del logo en el lienzo 108dp |
|---------|----------------|-------------------------------------|
| `drawable-nodpi/piku_logo_icon_adaptive.png` | API 29–30 (p. ej. Motorola G30) | ~20%, centrado |
| `drawable-nodpi-v31/piku_logo_icon_adaptive.png` | API 31+ (p. ej. Samsung A16) | ~32%, centrado |

`ic_launcher_foreground_piku.xml` y `Theme.Piku.Splash` apuntan a `@drawable/piku_logo_icon_adaptive`; el sistema elige el PNG según la API.

## Ajustar tamaños

Regenerar los PNG (PowerShell + `System.Drawing`) cambiando el porcentaje del lienzo (20 y 32 en el script del agente / build manual).

Tras cambiar: desinstalar la app e instalar de nuevo; borrar caché del launcher si el icono no actualiza.
