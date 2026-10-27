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
package rocks.urzahil.confnav.ui.icons.outlined

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

val Icons.Outlined.EditCalendar: ImageVector
    get() {
        if (_editCalendar != null) {
            return _editCalendar!!
        }
        _editCalendar = materialIcon(name = "Outlined.EditCalendar") {
            materialPath {
                moveTo(5.0f, 10.0f)
                horizontalLineToRelative(14.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(2.0f)
                verticalLineTo(6.0f)
                curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                horizontalLineToRelative(-1.0f)
                verticalLineTo(2.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(2.0f)
                horizontalLineTo(8.0f)
                verticalLineTo(2.0f)
                horizontalLineTo(6.0f)
                verticalLineToRelative(2.0f)
                horizontalLineTo(5.0f)
                curveTo(3.89f, 4.0f, 3.01f, 4.9f, 3.01f, 6.0f)
                lineTo(3.0f, 20.0f)
                curveToRelative(0.0f, 1.1f, 0.89f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(7.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineTo(5.0f)
                verticalLineTo(10.0f)
                close()
                moveTo(5.0f, 6.0f)
                horizontalLineToRelative(14.0f)
                verticalLineToRelative(2.0f)
                horizontalLineTo(5.0f)
                verticalLineTo(6.0f)
                close()
                moveTo(22.84f, 16.28f)
                lineToRelative(-0.71f, 0.71f)
                lineToRelative(-2.12f, -2.12f)
                lineToRelative(0.71f, -0.71f)
                curveToRelative(0.39f, -0.39f, 1.02f, -0.39f, 1.41f, 0.0f)
                lineToRelative(0.71f, 0.71f)
                curveTo(23.23f, 15.26f, 23.23f, 15.89f, 22.84f, 16.28f)
                close()
                moveTo(19.3f, 15.58f)
                lineToRelative(2.12f, 2.12f)
                lineToRelative(-5.3f, 5.3f)
                horizontalLineTo(14.0f)
                verticalLineToRelative(-2.12f)
                lineTo(19.3f, 15.58f)
                close()
            }
        }
        return _editCalendar!!
    }

private var _editCalendar: ImageVector? = null
