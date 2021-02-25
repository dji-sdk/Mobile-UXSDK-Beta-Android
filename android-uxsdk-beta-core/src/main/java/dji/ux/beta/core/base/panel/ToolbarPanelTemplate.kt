package dji.ux.beta.core.base.panel

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View

class ToolbarPanelTemplate {

    private var className: String? = null
    private var viewExists = false

    /**
     * Icon for the toolbar item
     */
    val toolbarIcon: Drawable?

    /**
     * Title for the toolbar item
     */
    val toolbarTitle: String?

    /**
     * Widget/View for toolbar content
     */
    var widget: View? = null

    /**
     * Flag for keeping the widget alive
     */
    val keepWidgetAlive: Boolean


    /**
     * Create Toolbar panel item using template
     *
     * Use this constructor to create a toolbar item when the view or widget
     * to be inserted in the toolbar panel is already instantiated.
     *
     * Toolbar requires barIcon or barTitle. In case both are null an exception will be thrown.
     *
     * @param icon - icon for toolbar
     * @param title - title for toolbar
     * @param widgetView - widget/view to be shown in the panel
     *
     */
    constructor(icon: Drawable?,
                title: String?,
                widgetView: View) {
        check(icon != null && title != null) {
            "Icon and title both cannot be null"
        }
        toolbarTitle = title
        toolbarIcon = icon
        widget = widgetView
        viewExists = true
        keepWidgetAlive = true
    }

    /**
     * Create Toolbar panel item using template
     *
     * Use this constructor to create a toolbar item when the view or widget
     * to be inserted in the toolbar panel should be instantiated dynamically.
     *
     * Toolbar requires barIcon or barTitle. In case both are null an exception will be thrown.
     *
     * @param icon - icon for toolbar
     * @param title - title for toolbar
     * @param className - string representing class name along with path including the package.
     * @param keepAlive - flag to keep the widget alive after instantiation
     */
    constructor(icon: Drawable?,
                title: String?,
                className: String,
                keepAlive: Boolean = true) {
        Class.forName(className)
        check(icon != null && title != null) {
            "Icon and title both cannot be null"
        }
        this.toolbarTitle = title
        this.toolbarIcon = icon
        this.className = className
        keepWidgetAlive = keepAlive
        viewExists = false
    }

    /**
     * Create view from class name.
     *
     * @param context - context for creating a widget
     */
    fun createFromClassName(context: Context, attributeSet: AttributeSet? = null): View {
        check(className != null) {
            "Class name is null"
        }
        val classFromName = Class.forName(className as String)
        val constructor = classFromName.constructors
        val widgetView = if (attributeSet != null) {
            constructor[1].newInstance(context, attributeSet)
        } else {
            constructor[0].newInstance(context)
        }
        check(widgetView is View) {
            "Class is not a view"
        }
        if (keepWidgetAlive) {
            widget = widgetView
            viewExists = true
        }
        return widgetView
    }

    /**
     * Check if view is created
     */
    fun doesViewExist(): Boolean {
        return viewExists
    }



}