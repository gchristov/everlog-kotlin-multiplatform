package com.everlog.ui.views

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.MenuItemCompat
import androidx.core.view.forEach
import com.everlog.R
import com.everlog.android.common.design.R as DesignR

/**
 * Opens a ⋮ button's menu below [anchor], the XML twin of the design system's AppMenu: styled by
 * AppTheme.PopupOverlay like the toolbars' overflow menus, with each option's icon in the content
 * colour. [build] adds the options, e.g. `inflate(R.menu.menu_row_plan_week)`.
 */
@SuppressLint("RestrictedApi")
fun showPopupMenu(anchor: View, build: PopupMenu.() -> Unit, onItemClick: (MenuItem) -> Unit) {
    val context = ContextThemeWrapper(anchor.context, DesignR.style.AppTheme_PopupOverlay)
    // Its end lines up with the button's, like the toolbars' menus
    val popup = PopupMenu(context, anchor, Gravity.END)
    popup.build()
    val tint = ColorStateList.valueOf(ContextCompat.getColor(context, DesignR.color.white_darker))
    popup.menu.forEach { MenuItemCompat.setIconTintList(it, tint) }
    popup.setOnMenuItemClickListener {
        onItemClick(it)
        false
    }
    // The icons only show through the helper
    MenuPopupHelper(context, popup.menu as MenuBuilder, anchor).apply {
        setForceShowIcon(true)
        gravity = Gravity.END
        show()
    }
}
