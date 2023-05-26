### Trinity supports the following keyboard controls

<details>
<summary>Application</summary>

| Event           | KeyCode             | Description                                                                                              |
|-----------------|---------------------|----------------------------------------------------------------------------------------------------------|
| Fullscreen Mode | ALT+ENTER           | Switches the entire application between Fullscreen (undecorated) and Windowed mode. Default is Windowed. |
| Shutdown        | CONTROL + SHIFT + C | Terminates the application including closing any open data sockets.                                      |

</details>
<details>
<summary>Hyperspace 3D Camera</summary>

| Event                 | KeyCode                                        | Description                                                                                                                                                                   |
|-----------------------|------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Reset View (Animated) | CONTROL + 0, CONTROL + NUMPAD0                 | Animates 3D Camera back to default orientation                                                                                                                                |
| Reset View (Instant)  | CONTROL + SHIFT + 0, CONTROL + SHIFT + NUMPAD0 | Instantly sets 3D Camera to default orientation                                                                                                                               |
| Zoom In               | W, SHIFT + W, CONTROL + W                      | Zooms 3D Camera forward (positive) on the Z axis. Shift increases the rate of change by an order of magnitude. Control decreases the rate of change by an order of magnitude  |
| Zoom Out              | S, SHIFT + S, CONTROL + S                      | Zooms 3D Camera backward (negative) on the Z axis. Shift increases the rate of change by an order of magnitude. Control decreases the rate of change by an order of magnitude |
| Pan Left              | A, SHIFT + A, CONTROL + A                      | Pans the 3D Camera left (negative) on the X axis. Shift increases the rate of change by an order of magnitude. Control decreases the rate of change by an order of magnitude  |
| Pan Right             | D, SHIFT + D, CONTROL + D                      | Pans the 3D Camera right (positive) on the X axis. Shift increases the rate of change by an order of magnitude. Control decreases the rate of change by an order of magnitude |
| Rotate Roll Negative  | CONTROL + 1, CONTROL + NUMPAD1                 | Rotates the 3D Camera negatively around the Z axis.                                                                                                                           |
| Rotate Roll Positive  | CONTROL + 3, CONTROL + NUMPAD3                 | Rotates the 3D Camera positively around the Z axis.                                                                                                                           |
| Rotate Pitch Negative | CONTROL + 4, CONTROL + NUMPAD4                 | Rotates the 3D Camera negatively around the X axis.                                                                                                                           |
| Rotate Pitch Positive | CONTROL + 6, CONTROL + NUMPAD6                 | Rotates the 3D Camera positively around the X axis.                                                                                                                           |
| Rotate Yaw Negative   | CONTROL + 7, CONTROL + NUMPAD7                 | Rotates the 3D Camera negatively around the Y axis.                                                                                                                           |
| Rotate Yaw Positively | CONTROL + 9, CONTROL + NUMPAD9                 | Rotates the 3D Camera Positively around the Y axis.                                                                                                                           |

</details>
<details>
<summary>Hyperspace Data</summary>

| Event                            | KeyCode                      | Description                                                                                                                                                                                                                          |
|----------------------------------|------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Slide Dimension Indices Forward  | Period (".")                 | Instantly shifts the x, y and z factor indices forward by 1. Redraws scene automatically. If any of the indices would exceed the maximum amount of available dimensions (feature vector) then no change is made.                     |
| Slide Dimension Indices Backward | Comma (",")                  | Instantly shifts the x, y and z factor indices backward by 1. Redraws scene automatically. If any of the indices would be less than 0 then no change is made.                                                                        |
| Debug Artifacts                  | Slash ("/")                  | Enables/Disables various visual nodes and labels useful for debugging data and renderings. Default is disabled.                                                                                                                      |
| Decrease Scatter Buffer          | "T"                          | Decreases scatter buff variable by 0.1. This decreases the range used for coordinate transformations. This has the effect of increasing the amount of space on screen the data takes up. Ignored when auto-scaling is enabled.       |
| Increase Scatter Buffer          | "Y"                          | Increases scatter buff scale variable by 0.1. This increases the range used for coordinate transformations. This has the effect of decreasing the amount of space on screen the data takes up. Ignored when auto-scaling is enabled. |
| Decrease Point Scale             | "U"                          | Decreases point scale variable by 0.1. This decreases a scalar value applied directly to the data prior to coordinate transformations. This has the effect of decreasing the amount of space on screen the data takes up.            |
| Increase Point Scale             | "I"                          | Increases point scale variable by 0.1. This increases a scalar value applied directly to the data prior to coordinate transformations. This has the effect of increasing the amount of space on screen the data takes up.            |
| Decrease Point Size              | "O"                          | Decreases the pointSize3D and pointSize2D variables by 5 pixels. This has the effect of decreasing the size of 3D tetrahedra in the scatter plot and the radius of 2D points in 2D projections.                                      |
| Increase Point Size              | "P"                          | Increases the pointSize3D and pointSize2D variables by 5 pixels. This has the effect of increasing the size of 3D tetrahedra in the scatter plot and the radius of 2D points in 2D projections.                                      |
| Blow out Cube Walls              | CONTROL + OPENBRACKET ("[")  | Animates the 2D projections out and away from the center of the scene to allow better viewing of the 3D scatter data.                                                                                                                |
| Contract in Cube Walls           | CONTROL + CLOSEBRACKET ("]") | Animates the 2D projections back to the default locations of the cube so that 2D Projections are aligned with the 3D scatter data.                                                                                                   |

</details>
