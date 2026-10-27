/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Copied from androidx.compose.material:material-icons-extended 1.7.0 (path data unchanged) so the app
// only needs material-icons-core.
package rocks.urzahil.confnav.ui.icons.filled

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

val Icons.Filled.Directions: ImageVector
    get() {
        if (_directions != null) {
            return _directions!!
        }
        _directions = materialIcon(name = "Filled.Directions") {
            materialPath {
                moveTo(21.41f, 10.59f)
                lineToRelative(-7.99f, -8.0f)
                curveToRelative(-0.78f, -0.78f, -2.05f, -0.78f, -2.83f, 0.0f)
                lineToRelative(-8.01f, 8.0f)
                curveToRelative(-0.78f, 0.78f, -0.78f, 2.05f, 0.0f, 2.83f)
                lineToRelative(8.01f, 8.0f)
                curveToRelative(0.78f, 0.78f, 2.05f, 0.78f, 2.83f, 0.0f)
                lineToRelative(7.99f, -8.0f)
                curveTo(22.2f, 12.63f, 22.2f, 11.37f, 21.41f, 10.59f)
                close()
                moveTo(13.5f, 14.5f)
                verticalLineTo(12.0f)
                horizontalLineTo(10.0f)
                verticalLineToRelative(3.0f)
                horizontalLineTo(8.0f)
                verticalLineToRelative(-4.0f)
                curveToRelative(0.0f, -0.55f, 0.45f, -1.0f, 1.0f, -1.0f)
                horizontalLineToRelative(4.5f)
                verticalLineTo(7.5f)
                lineTo(17.0f, 11.0f)
                lineTo(13.5f, 14.5f)
                close()
            }
        }
        return _directions!!
    }

private var _directions: ImageVector? = null
