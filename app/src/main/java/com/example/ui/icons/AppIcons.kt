package com.example.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object AppIcons {

    private fun buildIcon(name: String, pathBuilder: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit): ImageVector {
        return ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White), pathBuilder = pathBuilder)
        }.build()
    }

    val Bulb: ImageVector by lazy {
        buildIcon("Bulb") {
            moveTo(9f, 21f)
            lineTo(15f, 21f)
            lineTo(15f, 19f)
            lineTo(9f, 19f)
            close()
            moveTo(12f, 2f)
            curveTo(8.14f, 2f, 5f, 5.14f, 5f, 9f)
            curveTo(5f, 11.38f, 6.19f, 13.47f, 8f, 14.74f)
            lineTo(8f, 17f)
            lineTo(16f, 17f)
            lineTo(16f, 14.74f)
            curveTo(17.81f, 13.47f, 19f, 11.38f, 19f, 9f)
            curveTo(19f, 5.14f, 15.86f, 2f, 12f, 2f)
            close()
        }
    }

    val Fan: ImageVector by lazy {
        buildIcon("Fan") {
            moveTo(12f, 2f)
            curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
            curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
            curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
            curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
            close()
            moveTo(12f, 10f)
            curveTo(13.1f, 10f, 14f, 10.9f, 14f, 12f)
            curveTo(14f, 13.1f, 13.1f, 14f, 12f, 14f)
            curveTo(10.9f, 14f, 10f, 13.1f, 10f, 12f)
            curveTo(10f, 10.9f, 10.9f, 10f, 12f, 10f)
            close()
            moveTo(11f, 4f)
            lineTo(13f, 4f)
            lineTo(13f, 8f)
            lineTo(11f, 8f)
            close()
            moveTo(4f, 11f)
            lineTo(8f, 11f)
            lineTo(8f, 13f)
            lineTo(4f, 13f)
            close()
            moveTo(11f, 16f)
            lineTo(13f, 16f)
            lineTo(13f, 20f)
            lineTo(11f, 20f)
            close()
            moveTo(16f, 11f)
            lineTo(20f, 11f)
            lineTo(20f, 13f)
            lineTo(16f, 13f)
            close()
        }
    }

    val Plug: ImageVector by lazy {
        buildIcon("Plug") {
            moveTo(16f, 7f)
            lineTo(16f, 3f)
            lineTo(14f, 3f)
            lineTo(14f, 7f)
            lineTo(10f, 7f)
            lineTo(10f, 3f)
            lineTo(8f, 3f)
            lineTo(8f, 7f)
            curveTo(6.9f, 7f, 6f, 7.9f, 6f, 9f)
            lineTo(6f, 13f)
            curveTo(6f, 15.97f, 8.16f, 18.43f, 11f, 18.91f)
            lineTo(11f, 21f)
            lineTo(13f, 21f)
            lineTo(13f, 18.91f)
            curveTo(15.84f, 18.43f, 18f, 15.97f, 18f, 13f)
            lineTo(18f, 9f)
            curveTo(18f, 7.9f, 17.1f, 7f, 16f, 7f)
            close()
        }
    }

    val Tv: ImageVector by lazy {
        buildIcon("Tv") {
            moveTo(21f, 3f)
            lineTo(3f, 3f)
            curveTo(1.9f, 3f, 1f, 3.9f, 1f, 5f)
            lineTo(1f, 17f)
            curveTo(1f, 18.1f, 1.9f, 19f, 3f, 19f)
            lineTo(8f, 19f)
            lineTo(8f, 21f)
            lineTo(16f, 21f)
            lineTo(16f, 19f)
            lineTo(21f, 19f)
            curveTo(22.1f, 19f, 23f, 18.1f, 23f, 17f)
            lineTo(23f, 5f)
            curveTo(23f, 3.9f, 22.1f, 3f, 21f, 3f)
            close()
            moveTo(21f, 17f)
            lineTo(3f, 17f)
            lineTo(3f, 5f)
            lineTo(21f, 5f)
            lineTo(21f, 17f)
            close()
        }
    }

    val Lamp: ImageVector by lazy {
        buildIcon("Lamp") {
            moveTo(12f, 2f)
            lineTo(6f, 10f)
            lineTo(18f, 10f)
            close()
            moveTo(11f, 11f)
            lineTo(13f, 11f)
            lineTo(13f, 18f)
            lineTo(11f, 18f)
            close()
            moveTo(7f, 19f)
            lineTo(17f, 19f)
            lineTo(17f, 21f)
            lineTo(7f, 21f)
            close()
        }
    }

    val Ac: ImageVector by lazy {
        buildIcon("Ac") {
            moveTo(19f, 4f)
            lineTo(5f, 4f)
            curveTo(3.9f, 4f, 3f, 4.9f, 3f, 6f)
            lineTo(3f, 14f)
            curveTo(3f, 15.1f, 3.9f, 16f, 5f, 16f)
            lineTo(19f, 16f)
            curveTo(20.1f, 16f, 21f, 15.1f, 21f, 14f)
            lineTo(21f, 6f)
            curveTo(21f, 4.9f, 20.1f, 4f, 19f, 4f)
            close()
            moveTo(19f, 14f)
            lineTo(5f, 14f)
            lineTo(5f, 6f)
            lineTo(19f, 6f)
            lineTo(19f, 14f)
            close()
            moveTo(6f, 18f)
            lineTo(8f, 20f)
            lineTo(7f, 21f)
            lineTo(5f, 19f)
            close()
            moveTo(11f, 18f)
            lineTo(13f, 20f)
            lineTo(12f, 21f)
            lineTo(10f, 19f)
            close()
            moveTo(16f, 18f)
            lineTo(18f, 20f)
            lineTo(17f, 21f)
            lineTo(15f, 19f)
            close()
        }
    }

    val Heater: ImageVector by lazy {
        buildIcon("Heater") {
            moveTo(19f, 4f)
            lineTo(5f, 4f)
            curveTo(3.9f, 4f, 3f, 4.9f, 3f, 6f)
            lineTo(3f, 18f)
            curveTo(3f, 19.1f, 3.9f, 20f, 5f, 20f)
            lineTo(19f, 20f)
            curveTo(20.1f, 20f, 21f, 19.1f, 21f, 18f)
            lineTo(21f, 6f)
            curveTo(21f, 4.9f, 20.1f, 4f, 19f, 4f)
            close()
            moveTo(7f, 7f)
            lineTo(9f, 7f)
            lineTo(9f, 17f)
            lineTo(7f, 17f)
            close()
            moveTo(11f, 7f)
            lineTo(13f, 7f)
            lineTo(13f, 17f)
            lineTo(11f, 17f)
            close()
            moveTo(15f, 7f)
            lineTo(17f, 7f)
            lineTo(17f, 17f)
            lineTo(15f, 17f)
            close()
        }
    }

    val Speaker: ImageVector by lazy {
        buildIcon("Speaker") {
            moveTo(17f, 2f)
            lineTo(7f, 2f)
            curveTo(5.9f, 2f, 5f, 2.9f, 5f, 4f)
            lineTo(5f, 20f)
            curveTo(5f, 21.1f, 5.9f, 22f, 7f, 22f)
            lineTo(17f, 22f)
            curveTo(18.1f, 22f, 19f, 21.1f, 19f, 20f)
            lineTo(19f, 4f)
            curveTo(19f, 2.9f, 18.1f, 2f, 17f, 2f)
            close()
            moveTo(12f, 5f)
            curveTo(12.83f, 5f, 13.5f, 5.67f, 13.5f, 6.5f)
            curveTo(13.5f, 7.33f, 12.83f, 8f, 12f, 8f)
            curveTo(11.17f, 8f, 10.5f, 7.33f, 10.5f, 6.5f)
            curveTo(10.5f, 5.67f, 11.17f, 5f, 12f, 5f)
            close()
            moveTo(12f, 19f)
            curveTo(9.79f, 19f, 8f, 17.21f, 8f, 15f)
            curveTo(8f, 12.79f, 9.79f, 11f, 12f, 11f)
            curveTo(14.21f, 11f, 16f, 12.79f, 16f, 15f)
            curveTo(16f, 17.21f, 14.21f, 19f, 12f, 19f)
            close()
            moveTo(12f, 13.5f)
            curveTo(11.17f, 13.5f, 10.5f, 14.17f, 10.5f, 15f)
            curveTo(10.5f, 15.83f, 11.17f, 16.5f, 12f, 16.5f)
            curveTo(12.83f, 16.5f, 13.5f, 15.83f, 13.5f, 15f)
            curveTo(13.5f, 14.17f, 12.83f, 13.5f, 12f, 13.5f)
            close()
        }
    }

    val Power: ImageVector by lazy {
        buildIcon("Power") {
            moveTo(13f, 3f)
            lineTo(11f, 3f)
            lineTo(11f, 13f)
            lineTo(13f, 13f)
            close()
            moveTo(17.83f, 5.17f)
            lineTo(16.41f, 6.59f)
            curveTo(17.99f, 7.86f, 19f, 9.81f, 19f, 12f)
            curveTo(19f, 15.87f, 15.87f, 19f, 12f, 19f)
            curveTo(8.13f, 19f, 5f, 15.87f, 5f, 12f)
            curveTo(5f, 9.81f, 6.01f, 7.86f, 7.58f, 6.58f)
            lineTo(6.17f, 5.17f)
            curveTo(4.23f, 6.82f, 3f, 9.26f, 3f, 12f)
            curveTo(3f, 16.97f, 7.03f, 21f, 12f, 21f)
            curveTo(16.97f, 21f, 21f, 16.97f, 21f, 12f)
            curveTo(21f, 9.26f, 19.77f, 6.82f, 17.83f, 5.17f)
            close()
        }
    }

    val Settings: ImageVector by lazy {
        buildIcon("Settings") {
            moveTo(19.14f, 12.94f)
            curveTo(19.18f, 12.64f, 19.2f, 12.33f, 19.2f, 12f)
            curveTo(19.2f, 11.68f, 19.18f, 11.36f, 19.13f, 11.06f)
            lineTo(21.16f, 9.48f)
            curveTo(21.34f, 9.34f, 21.39f, 9.07f, 21.28f, 8.87f)
            lineTo(19.36f, 5.55f)
            curveTo(19.24f, 5.33f, 18.99f, 5.26f, 18.77f, 5.33f)
            lineTo(16.38f, 6.29f)
            curveTo(15.88f, 5.91f, 15.35f, 5.59f, 14.76f, 5.35f)
            lineTo(14.4f, 2.81f)
            curveTo(14.36f, 2.57f, 14.16f, 2.4f, 13.92f, 2.4f)
            lineTo(10.08f, 2.4f)
            curveTo(9.84f, 2.4f, 9.65f, 2.57f, 9.61f, 2.81f)
            lineTo(9.25f, 5.35f)
            curveTo(8.66f, 5.59f, 8.12f, 5.92f, 7.63f, 6.29f)
            lineTo(5.24f, 5.33f)
            curveTo(5.02f, 5.25f, 4.77f, 5.33f, 4.65f, 5.55f)
            lineTo(2.73f, 8.87f)
            curveTo(2.61f, 9.08f, 2.65f, 9.34f, 2.85f, 9.48f)
            lineTo(4.88f, 11.06f)
            curveTo(4.83f, 11.36f, 4.79f, 11.69f, 4.79f, 12f)
            curveTo(4.79f, 12.31f, 4.81f, 12.64f, 4.86f, 12.94f)
            lineTo(2.83f, 14.52f)
            curveTo(2.65f, 14.66f, 2.6f, 14.93f, 2.71f, 15.13f)
            lineTo(4.63f, 18.45f)
            curveTo(4.75f, 18.67f, 5f, 18.74f, 5.22f, 18.67f)
            lineTo(7.61f, 17.71f)
            curveTo(8.11f, 18.09f, 8.64f, 18.41f, 9.23f, 18.65f)
            lineTo(9.59f, 21.19f)
            curveTo(9.64f, 21.43f, 9.84f, 21.6f, 10.08f, 21.6f)
            lineTo(13.92f, 21.6f)
            curveTo(14.16f, 21.6f, 14.36f, 21.43f, 14.39f, 21.19f)
            lineTo(14.75f, 18.65f)
            curveTo(15.34f, 18.41f, 15.88f, 18.08f, 16.37f, 17.71f)
            lineTo(18.76f, 18.67f)
            curveTo(18.98f, 18.75f, 19.23f, 18.67f, 19.35f, 18.45f)
            lineTo(21.27f, 15.13f)
            curveTo(21.38f, 14.91f, 21.33f, 14.66f, 21.14f, 14.52f)
            close()
            moveTo(12f, 15.5f)
            curveTo(10.07f, 15.5f, 8.5f, 13.93f, 8.5f, 12f)
            curveTo(8.5f, 10.07f, 10.07f, 8.5f, 12f, 8.5f)
            curveTo(13.93f, 8.5f, 15.5f, 10.07f, 15.5f, 12f)
            curveTo(15.5f, 13.93f, 13.93f, 15.5f, 12f, 15.5f)
            close()
        }
    }

    val Scenes: ImageVector by lazy {
        buildIcon("Scenes") {
            moveTo(12f, 2f)
            lineTo(2f, 7f)
            lineTo(12f, 12f)
            lineTo(22f, 7f)
            close()
            moveTo(2f, 17f)
            lineTo(12f, 22f)
            lineTo(22f, 17f)
            lineTo(22f, 15f)
            lineTo(12f, 20f)
            lineTo(2f, 15f)
            close()
            moveTo(2f, 12f)
            lineTo(12f, 17f)
            lineTo(22f, 12f)
            lineTo(22f, 10f)
            lineTo(12f, 15f)
            lineTo(2f, 10f)
            close()
        }
    }

    val Add: ImageVector by lazy {
        buildIcon("Add") {
            moveTo(19f, 13f)
            lineTo(13f, 13f)
            lineTo(13f, 19f)
            lineTo(11f, 19f)
            lineTo(11f, 13f)
            lineTo(5f, 13f)
            lineTo(5f, 11f)
            lineTo(11f, 11f)
            lineTo(11f, 5f)
            lineTo(13f, 5f)
            lineTo(13f, 11f)
            lineTo(19f, 11f)
            close()
        }
    }

    val Delete: ImageVector by lazy {
        buildIcon("Delete") {
            moveTo(6f, 19f)
            curveTo(6f, 20.1f, 6.9f, 21f, 8f, 21f)
            lineTo(16f, 21f)
            curveTo(17.1f, 21f, 18f, 20.1f, 18f, 19f)
            lineTo(18f, 7f)
            lineTo(6f, 7f)
            close()
            moveTo(19f, 4f)
            lineTo(15.5f, 4f)
            lineTo(14.5f, 3f)
            lineTo(9.5f, 3f)
            lineTo(8.5f, 4f)
            lineTo(5f, 4f)
            lineTo(5f, 6f)
            lineTo(19f, 6f)
            close()
        }
    }

    val Edit: ImageVector by lazy {
        buildIcon("Edit") {
            moveTo(3f, 17.25f)
            lineTo(3f, 21f)
            lineTo(6.75f, 21f)
            lineTo(17.81f, 9.94f)
            lineTo(14.06f, 6.19f)
            close()
            moveTo(20.71f, 7.04f)
            curveTo(21.1f, 6.65f, 21.1f, 6.02f, 20.71f, 5.63f)
            lineTo(18.37f, 3.29f)
            curveTo(17.98f, 2.9f, 17.35f, 2.9f, 16.96f, 3.29f)
            lineTo(15.13f, 5.12f)
            lineTo(18.88f, 8.87f)
            close()
        }
    }

    val Fingerprint: ImageVector by lazy {
        buildIcon("Fingerprint") {
            moveTo(17.81f, 4.47f)
            curveTo(17.73f, 4.47f, 17.65f, 4.45f, 17.58f, 4.41f)
            curveTo(15.66f, 3.42f, 13.91f, 3f, 12f, 3f)
            curveTo(10.09f, 3f, 8.34f, 3.42f, 6.42f, 4.41f)
            curveTo(6.17f, 4.54f, 5.86f, 4.44f, 5.73f, 4.19f)
            curveTo(5.6f, 3.94f, 5.7f, 3.63f, 5.95f, 3.5f)
            curveTo(8.04f, 2.42f, 9.94f, 2f, 12f, 2f)
            curveTo(14.06f, 2f, 15.96f, 2.42f, 18.05f, 3.5f)
            curveTo(18.3f, 3.63f, 18.4f, 3.94f, 18.27f, 4.19f)
            curveTo(18.17f, 4.37f, 17.99f, 4.47f, 17.81f, 4.47f)
            close()
            moveTo(9.5f, 21.5f)
            curveTo(9.36f, 21.5f, 9.22f, 21.44f, 9.12f, 21.32f)
            curveTo(7.85f, 19.82f, 7.17f, 18.33f, 7.17f, 16.27f)
            curveTo(7.17f, 13.54f, 9.33f, 11.38f, 12.06f, 11.38f)
            curveTo(14.79f, 11.38f, 16.95f, 13.54f, 16.95f, 16.27f)
            curveTo(16.95f, 16.55f, 16.73f, 16.77f, 16.45f, 16.77f)
            curveTo(16.17f, 16.77f, 15.95f, 16.55f, 15.95f, 16.27f)
            curveTo(15.95f, 14.09f, 14.24f, 12.38f, 12.06f, 12.38f)
            curveTo(9.88f, 12.38f, 8.17f, 14.09f, 8.17f, 16.27f)
            curveTo(8.17f, 18.06f, 8.76f, 19.34f, 9.88f, 20.68f)
            close()
            moveTo(12f, 6.5f)
            curveTo(9.24f, 6.5f, 7f, 8.74f, 7f, 11.5f)
            curveTo(7f, 11.78f, 6.78f, 12f, 6.5f, 12f)
            curveTo(6.22f, 12f, 6f, 11.78f, 6f, 11.5f)
            curveTo(6f, 8.19f, 8.69f, 5.5f, 12f, 5.5f)
            curveTo(15.31f, 5.5f, 18f, 8.19f, 18f, 11.5f)
            curveTo(18f, 11.78f, 17.78f, 12f, 17.5f, 12f)
            curveTo(17.22f, 12f, 17f, 11.78f, 17f, 11.5f)
            curveTo(17f, 8.74f, 14.76f, 6.5f, 12f, 6.5f)
            close()
        }
    }

    val Lock: ImageVector by lazy {
        buildIcon("Lock") {
            moveTo(18f, 8f)
            lineTo(17f, 8f)
            lineTo(17f, 6f)
            curveTo(17f, 3.24f, 14.76f, 1f, 12f, 1f)
            curveTo(9.24f, 1f, 7f, 3.24f, 7f, 6f)
            lineTo(7f, 8f)
            lineTo(6f, 8f)
            curveTo(4.9f, 8f, 4f, 8.9f, 4f, 10f)
            lineTo(4f, 20f)
            curveTo(4f, 21.1f, 4.9f, 22f, 6f, 22f)
            lineTo(18f, 22f)
            curveTo(19.1f, 22f, 20f, 21.1f, 20f, 20f)
            lineTo(20f, 10f)
            curveTo(20f, 8.9f, 19.1f, 8f, 18f, 8f)
            close()
            moveTo(9f, 6f)
            curveTo(9f, 4.34f, 10.34f, 3f, 12f, 3f)
            curveTo(13.66f, 3f, 15f, 4.34f, 15f, 6f)
            lineTo(15f, 8f)
            lineTo(9f, 8f)
            close()
            moveTo(12f, 17f)
            curveTo(10.9f, 17f, 10f, 16.1f, 10f, 15f)
            curveTo(10f, 13.9f, 10.9f, 13f, 12f, 13f)
            curveTo(13.1f, 13f, 14f, 13.9f, 14f, 15f)
            curveTo(14f, 16.1f, 13.1f, 17f, 12f, 17f)
            close()
        }
    }

    val Visibility: ImageVector by lazy {
        buildIcon("Visibility") {
            moveTo(12f, 4.5f)
            curveTo(7f, 4.5f, 2.73f, 7.61f, 1f, 12f)
            curveTo(2.73f, 16.39f, 7f, 19.5f, 12f, 19.5f)
            curveTo(17f, 19.5f, 21.27f, 16.39f, 23f, 12f)
            curveTo(21.27f, 7.61f, 17f, 4.5f, 12f, 4.5f)
            close()
            moveTo(12f, 17f)
            curveTo(9.24f, 17f, 7f, 14.76f, 7f, 12f)
            curveTo(7f, 9.24f, 9.24f, 7f, 12f, 7f)
            curveTo(14.76f, 7f, 17f, 9.24f, 17f, 12f)
            curveTo(17f, 14.76f, 14.76f, 17f, 12f, 17f)
            close()
            moveTo(12f, 9f)
            curveTo(10.34f, 9f, 9f, 10.34f, 9f, 12f)
            curveTo(9f, 13.66f, 10.34f, 15f, 12f, 15f)
            curveTo(13.66f, 15f, 15f, 13.66f, 15f, 12f)
            curveTo(15f, 10.34f, 13.66f, 9f, 12f, 9f)
            close()
        }
    }

    val VisibilityOff: ImageVector by lazy {
        buildIcon("VisibilityOff") {
            moveTo(12f, 7f)
            curveTo(13.66f, 7f, 15f, 8.34f, 15f, 10f)
            lineTo(15f, 10.18f)
            lineTo(16.98f, 12.16f)
            curveTo(16.99f, 12.11f, 17f, 12.06f, 17f, 12f)
            curveTo(17f, 9.24f, 14.76f, 7f, 12f, 7f)
            close()
            moveTo(2f, 4.27f)
            lineTo(4.28f, 6.55f)
            lineTo(4.74f, 7.01f)
            curveTo(3.08f, 8.3f, 1.78f, 10f, 1f, 12f)
            curveTo(2.73f, 16.39f, 7f, 19.5f, 12f, 19.5f)
            curveTo(13.55f, 19.5f, 15.03f, 19.2f, 16.38f, 18.66f)
            lineTo(16.82f, 19.1f)
            lineTo(19.73f, 22f)
            lineTo(21f, 20.73f)
            lineTo(3.27f, 3f)
            close()
        }
    }

    val ArrowUp: ImageVector by lazy {
        buildIcon("ArrowUp") {
            moveTo(7.41f, 15.41f)
            lineTo(12f, 10.83f)
            lineTo(16.59f, 15.41f)
            lineTo(18f, 14f)
            lineTo(12f, 8f)
            lineTo(6f, 14f)
            close()
        }
    }

    val ArrowDown: ImageVector by lazy {
        buildIcon("ArrowDown") {
            moveTo(7.41f, 8.59f)
            lineTo(12f, 13.17f)
            lineTo(16.59f, 8.59f)
            lineTo(18f, 10f)
            lineTo(12f, 16f)
            lineTo(6f, 10f)
            close()
        }
    }

    val Check: ImageVector by lazy {
        buildIcon("Check") {
            moveTo(9f, 16.17f)
            lineTo(4.83f, 12f)
            lineTo(3.41f, 13.41f)
            lineTo(9f, 19f)
            lineTo(21f, 7f)
            lineTo(19.59f, 5.59f)
            close()
        }
    }

    val Logout: ImageVector by lazy {
        buildIcon("Logout") {
            moveTo(17f, 7f)
            lineTo(15.59f, 8.41f)
            lineTo(17.17f, 10f)
            lineTo(9f, 10f)
            lineTo(9f, 12f)
            lineTo(17.17f, 12f)
            lineTo(15.59f, 13.58f)
            lineTo(17f, 15f)
            lineTo(21f, 11f)
            close()
            moveTo(5f, 5f)
            lineTo(12f, 5f)
            lineTo(12f, 3f)
            lineTo(5f, 3f)
            curveTo(3.9f, 3f, 3f, 3.9f, 3f, 5f)
            lineTo(3f, 19f)
            curveTo(3f, 20.1f, 3.9f, 21f, 5f, 21f)
            lineTo(12f, 21f)
            lineTo(12f, 19f)
            lineTo(5f, 19f)
            close()
        }
    }

    fun getSwitchIcon(iconKey: String): ImageVector {
        return when (iconKey.lowercase()) {
            "bulb" -> Bulb
            "fan" -> Fan
            "plug" -> Plug
            "tv" -> Tv
            "lamp" -> Lamp
            "ac" -> Ac
            "heater" -> Heater
            "speaker" -> Speaker
            else -> Bulb
        }
    }

    val AVAILABLE_SWITCH_ICONS = listOf(
        "bulb" to Bulb,
        "fan" to Fan,
        "plug" to Plug,
        "tv" to Tv,
        "lamp" to Lamp,
        "ac" to Ac,
        "heater" to Heater,
        "speaker" to Speaker
    )
}
