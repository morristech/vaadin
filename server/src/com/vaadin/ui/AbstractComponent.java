/*
 * Copyright 2011 Vaadin Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.vaadin.ui;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vaadin.event.ActionManager;
import com.vaadin.event.EventRouter;
import com.vaadin.event.MethodEventSource;
import com.vaadin.event.ShortcutListener;
import com.vaadin.server.AbstractClientConnector;
import com.vaadin.server.ClientConnector;
import com.vaadin.server.ComponentSizeValidator;
import com.vaadin.server.ErrorMessage;
import com.vaadin.server.Resource;
import com.vaadin.server.Terminal;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.ComponentConstants;
import com.vaadin.shared.ComponentState;
import com.vaadin.shared.ui.ComponentStateUtil;
import com.vaadin.util.ReflectTools;

/**
 * An abstract class that defines default implementation for the
 * {@link Component} interface. Basic UI components that are not derived from an
 * external component can inherit this class to easily qualify as Vaadin
 * components. Most components in Vaadin do just that.
 * 
 * @author Vaadin Ltd.
 * @since 3.0
 */
@SuppressWarnings("serial")
public abstract class AbstractComponent extends AbstractClientConnector
        implements Component, MethodEventSource {

    /* Private members */

    /**
     * Application specific data object. The component does not use or modify
     * this.
     */
    private Object applicationData;

    /**
     * The EventRouter used for the event model.
     * 
     * @deprecated The EventRouter will be replaced by an EventRouter which does
     *             not use reflection in Vaadin 7.0.0. See #8640.
     */
    @Deprecated
    private EventRouter eventRouter = null;

    /**
     * The internal error message of the component.
     */
    private ErrorMessage componentError = null;

    /**
     * Locale of this component.
     */
    private Locale locale;

    /**
     * The component should receive focus (if {@link Focusable}) when attached.
     */
    private boolean delayedFocus;

    /* Sizeable fields */

    private float width = SIZE_UNDEFINED;
    private float height = SIZE_UNDEFINED;
    private Unit widthUnit = Unit.PIXELS;
    private Unit heightUnit = Unit.PIXELS;
    private static final Pattern sizePattern = Pattern
            .compile("^(-?\\d+(\\.\\d+)?)(%|px|em|ex|in|cm|mm|pt|pc)?$");

    private ComponentErrorHandler errorHandler = null;

    /**
     * Keeps track of the Actions added to this component; the actual
     * handling/notifying is delegated, usually to the containing window.
     */
    private ActionManager actionManager;

    /* Constructor */

    /**
     * Constructs a new Component.
     */
    public AbstractComponent() {
        // ComponentSizeValidator.setCreationLocation(this);
    }

    /* Get/Set component properties */

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.ui.Component#setId(java.lang.String)
     */
    @Override
    public void setId(String id) {
        getState().id = id;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.ui.Component#getId()
     */
    @Override
    public String getId() {
        return getState().id;
    }

    /**
     * @deprecated as of 7.0. Use {@link #setId(String)}
     */
    @Deprecated
    public void setDebugId(String id) {
        setId(id);
    }

    /**
     * @deprecated as of 7.0. Use {@link #getId()}
     */
    @Deprecated
    public String getDebugId() {
        return getId();
    }

    /*
     * Gets the component's style. Don't add a JavaDoc comment here, we use the
     * default documentation from implemented interface.
     */
    @Override
    public String getStyleName() {
        String s = "";
        if (ComponentStateUtil.hasStyles(getState())) {
            for (final Iterator<String> it = getState().styles.iterator(); it
                    .hasNext();) {
                s += it.next();
                if (it.hasNext()) {
                    s += " ";
                }
            }
        }
        return s;
    }

    /*
     * Sets the component's style. Don't add a JavaDoc comment here, we use the
     * default documentation from implemented interface.
     */
    @Override
    public void setStyleName(String style) {
        if (style == null || "".equals(style)) {
            getState().styles = null;
            return;
        }
        if (getState().styles == null) {
            getState().styles = new ArrayList<String>();
        }
        List<String> styles = getState().styles;
        styles.clear();
        String[] styleParts = style.split(" +");
        for (String part : styleParts) {
            if (part.length() > 0) {
                styles.add(part);
            }
        }
    }

    @Override
    public void addStyleName(String style) {
        if (style == null || "".equals(style)) {
            return;
        }
        if (style.contains(" ")) {
            // Split space separated style names and add them one by one.
            for (String realStyle : style.split(" ")) {
                addStyleName(realStyle);
            }
            return;
        }

        if (getState().styles == null) {
            getState().styles = new ArrayList<String>();
        }
        List<String> styles = getState().styles;
        if (!styles.contains(style)) {
            styles.add(style);
        }
    }

    @Override
    public void removeStyleName(String style) {
        if (ComponentStateUtil.hasStyles(getState())) {
            String[] styleParts = style.split(" +");
            for (String part : styleParts) {
                if (part.length() > 0) {
                    getState().styles.remove(part);
                }
            }
        }
    }

    /*
     * Get's the component's caption. Don't add a JavaDoc comment here, we use
     * the default documentation from implemented interface.
     */
    @Override
    public String getCaption() {
        return getState().caption;
    }

    /**
     * Sets the component's caption <code>String</code>. Caption is the visible
     * name of the component. This method will trigger a
     * {@link RepaintRequestEvent}.
     * 
     * @param caption
     *            the new caption <code>String</code> for the component.
     */
    @Override
    public void setCaption(String caption) {
        getState().caption = caption;
    }

    /*
     * Don't add a JavaDoc comment here, we use the default documentation from
     * implemented interface.
     */
    @Override
    public Locale getLocale() {
        if (locale != null) {
            return locale;
        }
        HasComponents parent = getParent();
        if (parent != null) {
            return parent.getLocale();
        }
        final VaadinSession session = getSession();
        if (session != null) {
            return session.getLocale();
        }
        return null;
    }

    /**
     * Sets the locale of this component.
     * 
     * <pre>
     * // Component for which the locale is meaningful
     * InlineDateField date = new InlineDateField(&quot;Datum&quot;);
     * 
     * // German language specified with ISO 639-1 language
     * // code and ISO 3166-1 alpha-2 country code.
     * date.setLocale(new Locale(&quot;de&quot;, &quot;DE&quot;));
     * 
     * date.setResolution(DateField.RESOLUTION_DAY);
     * layout.addComponent(date);
     * </pre>
     * 
     * 
     * @param locale
     *            the locale to become this component's locale.
     */
    public void setLocale(Locale locale) {
        this.locale = locale;

        // FIXME: Reload value if there is a converter
        markAsDirty();
    }

    /*
     * Gets the component's icon resource. Don't add a JavaDoc comment here, we
     * use the default documentation from implemented interface.
     */
    @Override
    public Resource getIcon() {
        return getResource(ComponentConstants.ICON_RESOURCE);
    }

    /**
     * Sets the component's icon. This method will trigger a
     * {@link RepaintRequestEvent}.
     * 
     * @param icon
     *            the icon to be shown with the component's caption.
     */
    @Override
    public void setIcon(Resource icon) {
        setResource(ComponentConstants.ICON_RESOURCE, icon);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.ui.Component#isEnabled()
     */
    @Override
    public boolean isEnabled() {
        return getState().enabled;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.ui.Component#setEnabled(boolean)
     */
    @Override
    public void setEnabled(boolean enabled) {
        getState().enabled = enabled;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.client.Connector#isConnectorEnabled()
     */
    @Override
    public boolean isConnectorEnabled() {
        if (!isVisible()) {
            return false;
        } else if (!isEnabled()) {
            return false;
        } else if (!super.isConnectorEnabled()) {
            return false;
        } else if (!getParent().isComponentVisible(this)) {
            return false;
        } else {
            return true;
        }
    }

    /*
     * Tests if the component is in the immediate mode. Don't add a JavaDoc
     * comment here, we use the default documentation from implemented
     * interface.
     */
    public boolean isImmediate() {
        return getState().immediate;
    }

    /**
     * Sets the component's immediate mode to the specified status. This method
     * will trigger a {@link RepaintRequestEvent}.
     * 
     * @param immediate
     *            the boolean value specifying if the component should be in the
     *            immediate mode after the call.
     * @see Component#isImmediate()
     */
    public void setImmediate(boolean immediate) {
        getState().immediate = immediate;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.ui.Component#isVisible()
     */
    @Override
    public boolean isVisible() {
        return getState().visible;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.ui.Component#setVisible(boolean)
     */
    @Override
    public void setVisible(boolean visible) {
        if (getState().visible == visible) {
            return;
        }

        getState().visible = visible;
        if (getParent() != null) {
            // Must always repaint the parent (at least the hierarchy) when
            // visibility of a child component changes.
            getParent().markAsDirty();
        }
    }

    /**
     * <p>
     * Gets the component's description, used in tooltips and can be displayed
     * directly in certain other components such as forms. The description can
     * be used to briefly describe the state of the component to the user. The
     * description string may contain certain XML tags:
     * </p>
     * 
     * <p>
     * <table border=1>
     * <tr>
     * <td width=120><b>Tag</b></td>
     * <td width=120><b>Description</b></td>
     * <td width=120><b>Example</b></td>
     * </tr>
     * <tr>
     * <td>&lt;b></td>
     * <td>bold</td>
     * <td><b>bold text</b></td>
     * </tr>
     * <tr>
     * <td>&lt;i></td>
     * <td>italic</td>
     * <td><i>italic text</i></td>
     * </tr>
     * <tr>
     * <td>&lt;u></td>
     * <td>underlined</td>
     * <td><u>underlined text</u></td>
     * </tr>
     * <tr>
     * <td>&lt;br></td>
     * <td>linebreak</td>
     * <td>N/A</td>
     * </tr>
     * <tr>
     * <td>&lt;ul><br>
     * &lt;li>item1<br>
     * &lt;li>item1<br>
     * &lt;/ul></td>
     * <td>item list</td>
     * <td>
     * <ul>
     * <li>item1
     * <li>item2
     * </ul>
     * </td>
     * </tr>
     * </table>
     * </p>
     * 
     * <p>
     * These tags may be nested.
     * </p>
     * 
     * @return component's description <code>String</code>
     */
    public String getDescription() {
        return getState().description;
    }

    /**
     * Sets the component's description. See {@link #getDescription()} for more
     * information on what the description is. This method will trigger a
     * {@link RepaintRequestEvent}.
     * 
     * The description is displayed as HTML/XHTML in tooltips or directly in
     * certain components so care should be taken to avoid creating the
     * possibility for HTML injection and possibly XSS vulnerabilities.
     * 
     * @param description
     *            the new description string for the component.
     */
    public void setDescription(String description) {
        getState().description = description;
    }

    /*
     * Gets the component's parent component. Don't add a JavaDoc comment here,
     * we use the default documentation from implemented interface.
     */
    @Override
    public HasComponents getParent() {
        return (HasComponents) super.getParent();
    }

    @Override
    public void setParent(ClientConnector parent) {
        if (parent == null || parent instanceof HasComponents) {
            super.setParent(parent);
        } else {
            throw new IllegalArgumentException(
                    "The parent of a Component must implement HasComponents, which "
                            + parent.getClass() + " doesn't do.");
        }
    }

    /**
     * Returns the closest ancestor with the given type.
     * <p>
     * To find the Window that contains the component, use {@code Window w =
     * getParent(Window.class);}
     * </p>
     * 
     * @param <T>
     *            The type of the ancestor
     * @param parentType
     *            The ancestor class we are looking for
     * @return The first ancestor that can be assigned to the given class. Null
     *         if no ancestor with the correct type could be found.
     */
    public <T extends HasComponents> T findAncestor(Class<T> parentType) {
        HasComponents p = getParent();
        while (p != null) {
            if (parentType.isAssignableFrom(p.getClass())) {
                return parentType.cast(p);
            }
            p = p.getParent();
        }
        return null;
    }

    /**
     * Gets the error message for this component.
     * 
     * @return ErrorMessage containing the description of the error state of the
     *         component or null, if the component contains no errors. Extending
     *         classes should override this method if they support other error
     *         message types such as validation errors or buffering errors. The
     *         returned error message contains information about all the errors.
     */
    public ErrorMessage getErrorMessage() {
        return componentError;
    }

    /**
     * Gets the component's error message.
     * 
     * @link Terminal.ErrorMessage#ErrorMessage(String, int)
     * 
     * @return the component's error message.
     */
    public ErrorMessage getComponentError() {
        return componentError;
    }

    /**
     * Sets the component's error message. The message may contain certain XML
     * tags, for more information see
     * 
     * @link Component.ErrorMessage#ErrorMessage(String, int)
     * 
     * @param componentError
     *            the new <code>ErrorMessage</code> of the component.
     */
    public void setComponentError(ErrorMessage componentError) {
        this.componentError = componentError;
        fireComponentErrorEvent();
        markAsDirty();
    }

    /*
     * Tests if the component is in read-only mode. Don't add a JavaDoc comment
     * here, we use the default documentation from implemented interface.
     */
    @Override
    public boolean isReadOnly() {
        return getState().readOnly;
    }

    /*
     * Sets the component's read-only mode. Don't add a JavaDoc comment here, we
     * use the default documentation from implemented interface.
     */
    @Override
    public void setReadOnly(boolean readOnly) {
        getState().readOnly = readOnly;
    }

    /*
     * Notify the component that it's attached to a window. Don't add a JavaDoc
     * comment here, we use the default documentation from implemented
     * interface.
     */
    @Override
    public void attach() {
        super.attach();
        if (delayedFocus) {
            focus();
        }
        setActionManagerViewer();
    }

    /*
     * Detach the component from application. Don't add a JavaDoc comment here,
     * we use the default documentation from implemented interface.
     */
    @Override
    public void detach() {
        super.detach();
        if (actionManager != null) {
            // Remove any existing viewer. UI cast is just to make the
            // compiler happy
            actionManager.setViewer((UI) null);
        }
    }

    /**
     * Sets the focus for this component if the component is {@link Focusable}.
     */
    protected void focus() {
        if (this instanceof Focusable) {
            final VaadinSession session = getSession();
            if (session != null) {
                getUI().setFocusedComponent((Focusable) this);
                delayedFocus = false;
            } else {
                delayedFocus = true;
            }
        }
    }

    /**
     * Build CSS compatible string representation of height.
     * 
     * @return CSS height
     */
    private String getCSSHeight() {
        if (getHeightUnits() == Unit.PIXELS) {
            return ((int) getHeight()) + getHeightUnits().getSymbol();
        } else {
            return getHeight() + getHeightUnits().getSymbol();
        }
    }

    /**
     * Build CSS compatible string representation of width.
     * 
     * @return CSS width
     */
    private String getCSSWidth() {
        if (getWidthUnits() == Unit.PIXELS) {
            return ((int) getWidth()) + getWidthUnits().getSymbol();
        } else {
            return getWidth() + getWidthUnits().getSymbol();
        }
    }

    /**
     * Returns the shared state bean with information to be sent from the server
     * to the client.
     * 
     * Subclasses should override this method and set any relevant fields of the
     * state returned by super.getState().
     * 
     * @since 7.0
     * 
     * @return updated component shared state
     */
    @Override
    protected ComponentState getState() {
        return (ComponentState) super.getState();
    }

    @Override
    public void beforeClientResponse(boolean initial) {
        super.beforeClientResponse(initial);
        // TODO This logic should be on the client side and the state should
        // simply be a data object with "width" and "height".
        if (getHeight() >= 0
                && (getHeightUnits() != Unit.PERCENTAGE || ComponentSizeValidator
                        .parentCanDefineHeight(this))) {
            getState().height = "" + getCSSHeight();
        } else {
            getState().height = "";
        }

        if (getWidth() >= 0
                && (getWidthUnits() != Unit.PERCENTAGE || ComponentSizeValidator
                        .parentCanDefineWidth(this))) {
            getState().width = "" + getCSSWidth();
        } else {
            getState().width = "";
        }

        ErrorMessage error = getErrorMessage();
        if (null != error) {
            getState().errorMessage = error.getFormattedHtmlMessage();
        } else {
            getState().errorMessage = null;
        }
    }

    /* General event framework */

    private static final Method COMPONENT_EVENT_METHOD = ReflectTools
            .findMethod(Component.Listener.class, "componentEvent",
                    Component.Event.class);

    /**
     * <p>
     * Registers a new listener with the specified activation method to listen
     * events generated by this component. If the activation method does not
     * have any arguments the event object will not be passed to it when it's
     * called.
     * </p>
     * 
     * <p>
     * This method additionally informs the event-api to route events with the
     * given eventIdentifier to the components handleEvent function call.
     * </p>
     * 
     * <p>
     * For more information on the inheritable event mechanism see the
     * {@link com.vaadin.event com.vaadin.event package documentation}.
     * </p>
     * 
     * @param eventIdentifier
     *            the identifier of the event to listen for
     * @param eventType
     *            the type of the listened event. Events of this type or its
     *            subclasses activate the listener.
     * @param target
     *            the object instance who owns the activation method.
     * @param method
     *            the activation method.
     * 
     * @since 6.2
     * @deprecated The EventRouter will be replaced by an EventRouter which does
     *             not use reflection in Vaadin 7.0.0. See #8640.
     */
    @Deprecated
    protected void addListener(String eventIdentifier, Class<?> eventType,
            Object target, Method method) {
        if (eventRouter == null) {
            eventRouter = new EventRouter();
        }
        boolean needRepaint = !eventRouter.hasListeners(eventType);
        eventRouter.addListener(eventType, target, method);

        if (needRepaint) {
            ComponentStateUtil.addRegisteredEventListener(getState(),
                    eventIdentifier);
        }
    }

    /**
     * Checks if the given {@link Event} type is listened for this component.
     * 
     * @param eventType
     *            the event type to be checked
     * @return true if a listener is registered for the given event type
     */
    protected boolean hasListeners(Class<?> eventType) {
        return eventRouter != null && eventRouter.hasListeners(eventType);
    }

    /**
     * Removes all registered listeners matching the given parameters. Since
     * this method receives the event type and the listener object as
     * parameters, it will unregister all <code>object</code>'s methods that are
     * registered to listen to events of type <code>eventType</code> generated
     * by this component.
     * 
     * <p>
     * This method additionally informs the event-api to stop routing events
     * with the given eventIdentifier to the components handleEvent function
     * call.
     * </p>
     * 
     * <p>
     * For more information on the inheritable event mechanism see the
     * {@link com.vaadin.event com.vaadin.event package documentation}.
     * </p>
     * 
     * @param eventIdentifier
     *            the identifier of the event to stop listening for
     * @param eventType
     *            the exact event type the <code>object</code> listens to.
     * @param target
     *            the target object that has registered to listen to events of
     *            type <code>eventType</code> with one or more methods.
     * 
     * @since 6.2
     */
    protected void removeListener(String eventIdentifier, Class<?> eventType,
            Object target) {
        if (eventRouter != null) {
            eventRouter.removeListener(eventType, target);
            if (!eventRouter.hasListeners(eventType)) {
                ComponentStateUtil.removeRegisteredEventListener(getState(),
                        eventIdentifier);
            }
        }
    }

    /**
     * <p>
     * Registers a new listener with the specified activation method to listen
     * events generated by this component. If the activation method does not
     * have any arguments the event object will not be passed to it when it's
     * called.
     * </p>
     * 
     * <p>
     * For more information on the inheritable event mechanism see the
     * {@link com.vaadin.event com.vaadin.event package documentation}.
     * </p>
     * 
     * @param eventType
     *            the type of the listened event. Events of this type or its
     *            subclasses activate the listener.
     * @param target
     *            the object instance who owns the activation method.
     * @param method
     *            the activation method.
     * 
     * @deprecated The EventRouter will be replaced by an EventRouter which does
     *             not use reflection in Vaadin 7.0.0. See #8640.
     */
    @Override
    @Deprecated
    public void addListener(Class<?> eventType, Object target, Method method) {
        if (eventRouter == null) {
            eventRouter = new EventRouter();
        }
        eventRouter.addListener(eventType, target, method);
    }

    /**
     * <p>
     * Convenience method for registering a new listener with the specified
     * activation method to listen events generated by this component. If the
     * activation method does not have any arguments the event object will not
     * be passed to it when it's called.
     * </p>
     * 
     * <p>
     * This version of <code>addListener</code> gets the name of the activation
     * method as a parameter. The actual method is reflected from
     * <code>object</code>, and unless exactly one match is found,
     * <code>java.lang.IllegalArgumentException</code> is thrown.
     * </p>
     * 
     * <p>
     * For more information on the inheritable event mechanism see the
     * {@link com.vaadin.event com.vaadin.event package documentation}.
     * </p>
     * 
     * <p>
     * Note: Using this method is discouraged because it cannot be checked
     * during compilation. Use {@link #addListener(Class, Object, Method)} or
     * {@link #addListener(com.vaadin.ui.Component.Listener)} instead.
     * </p>
     * 
     * @param eventType
     *            the type of the listened event. Events of this type or its
     *            subclasses activate the listener.
     * @param target
     *            the object instance who owns the activation method.
     * @param methodName
     *            the name of the activation method.
     * @deprecated The EventRouter will be replaced by an EventRouter which does
     *             not use reflection in Vaadin 7.0.0. See #8640.
     */
    @Override
    @Deprecated
    public void addListener(Class<?> eventType, Object target, String methodName) {
        if (eventRouter == null) {
            eventRouter = new EventRouter();
        }
        eventRouter.addListener(eventType, target, methodName);
    }

    /**
     * Removes all registered listeners matching the given parameters. Since
     * this method receives the event type and the listener object as
     * parameters, it will unregister all <code>object</code>'s methods that are
     * registered to listen to events of type <code>eventType</code> generated
     * by this component.
     * 
     * <p>
     * For more information on the inheritable event mechanism see the
     * {@link com.vaadin.event com.vaadin.event package documentation}.
     * </p>
     * 
     * @param eventType
     *            the exact event type the <code>object</code> listens to.
     * @param target
     *            the target object that has registered to listen to events of
     *            type <code>eventType</code> with one or more methods.
     */
    @Override
    public void removeListener(Class<?> eventType, Object target) {
        if (eventRouter != null) {
            eventRouter.removeListener(eventType, target);
        }
    }

    /**
     * Removes one registered listener method. The given method owned by the
     * given object will no longer be called when the specified events are
     * generated by this component.
     * 
     * <p>
     * For more information on the inheritable event mechanism see the
     * {@link com.vaadin.event com.vaadin.event package documentation}.
     * </p>
     * 
     * @param eventType
     *            the exact event type the <code>object</code> listens to.
     * @param target
     *            target object that has registered to listen to events of type
     *            <code>eventType</code> with one or more methods.
     * @param method
     *            the method owned by <code>target</code> that's registered to
     *            listen to events of type <code>eventType</code>.
     * @deprecated The EventRouter will be replaced by an EventRouter which does
     *             not use reflection in Vaadin 7.0.0. See #8640.
     */
    @Override
    @Deprecated
    public void removeListener(Class<?> eventType, Object target, Method method) {
        if (eventRouter != null) {
            eventRouter.removeListener(eventType, target, method);
        }
    }

    /**
     * <p>
     * Removes one registered listener method. The given method owned by the
     * given object will no longer be called when the specified events are
     * generated by this component.
     * </p>
     * 
     * <p>
     * This version of <code>removeListener</code> gets the name of the
     * activation method as a parameter. The actual method is reflected from
     * <code>target</code>, and unless exactly one match is found,
     * <code>java.lang.IllegalArgumentException</code> is thrown.
     * </p>
     * 
     * <p>
     * For more information on the inheritable event mechanism see the
     * {@link com.vaadin.event com.vaadin.event package documentation}.
     * </p>
     * 
     * @param eventType
     *            the exact event type the <code>object</code> listens to.
     * @param target
     *            the target object that has registered to listen to events of
     *            type <code>eventType</code> with one or more methods.
     * @param methodName
     *            the name of the method owned by <code>target</code> that's
     *            registered to listen to events of type <code>eventType</code>.
     */
    @Override
    public void removeListener(Class<?> eventType, Object target,
            String methodName) {
        if (eventRouter != null) {
            eventRouter.removeListener(eventType, target, methodName);
        }
    }

    /**
     * Returns all listeners that are registered for the given event type or one
     * of its subclasses.
     * 
     * @param eventType
     *            The type of event to return listeners for.
     * @return A collection with all registered listeners. Empty if no listeners
     *         are found.
     */
    public Collection<?> getListeners(Class<?> eventType) {
        if (eventRouter == null) {
            return Collections.EMPTY_LIST;
        }

        return eventRouter.getListeners(eventType);
    }

    /**
     * Sends the event to all listeners.
     * 
     * @param event
     *            the Event to be sent to all listeners.
     */
    protected void fireEvent(Component.Event event) {
        if (eventRouter != null) {
            eventRouter.fireEvent(event);
        }

    }

    /* Component event framework */

    /*
     * Registers a new listener to listen events generated by this component.
     * Don't add a JavaDoc comment here, we use the default documentation from
     * implemented interface.
     */
    @Override
    public void addListener(Component.Listener listener) {
        addListener(Component.Event.class, listener, COMPONENT_EVENT_METHOD);
    }

    /*
     * Removes a previously registered listener from this component. Don't add a
     * JavaDoc comment here, we use the default documentation from implemented
     * interface.
     */
    @Override
    public void removeListener(Component.Listener listener) {
        removeListener(Component.Event.class, listener, COMPONENT_EVENT_METHOD);
    }

    /**
     * Emits the component event. It is transmitted to all registered listeners
     * interested in such events.
     */
    protected void fireComponentEvent() {
        fireEvent(new Component.Event(this));
    }

    /**
     * Emits the component error event. It is transmitted to all registered
     * listeners interested in such events.
     */
    protected void fireComponentErrorEvent() {
        fireEvent(new Component.ErrorEvent(getComponentError(), this));
    }

    /**
     * Sets the data object, that can be used for any application specific data.
     * The component does not use or modify this data.
     * 
     * @param data
     *            the Application specific data.
     * @since 3.1
     */
    public void setData(Object data) {
        applicationData = data;
    }

    /**
     * Gets the application specific data. See {@link #setData(Object)}.
     * 
     * @return the Application specific data set with setData function.
     * @since 3.1
     */
    public Object getData() {
        return applicationData;
    }

    /* Sizeable and other size related methods */

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.Sizeable#getHeight()
     */
    @Override
    public float getHeight() {
        return height;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.server.Sizeable#getHeightUnits()
     */
    @Override
    public Unit getHeightUnits() {
        return heightUnit;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.server.Sizeable#getWidth()
     */
    @Override
    public float getWidth() {
        return width;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.server.Sizeable#getWidthUnits()
     */
    @Override
    public Unit getWidthUnits() {
        return widthUnit;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.server.Sizeable#setHeight(float, Unit)
     */
    @Override
    public void setHeight(float height, Unit unit) {
        if (unit == null) {
            throw new IllegalArgumentException("Unit can not be null");
        }
        this.height = height;
        heightUnit = unit;
        markAsDirty();
        // ComponentSizeValidator.setHeightLocation(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.server.Sizeable#setSizeFull()
     */
    @Override
    public void setSizeFull() {
        setWidth(100, Unit.PERCENTAGE);
        setHeight(100, Unit.PERCENTAGE);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.server.Sizeable#setSizeUndefined()
     */
    @Override
    public void setSizeUndefined() {
        setWidth(-1, Unit.PIXELS);
        setHeight(-1, Unit.PIXELS);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.server.Sizeable#setWidth(float, Unit)
     */
    @Override
    public void setWidth(float width, Unit unit) {
        if (unit == null) {
            throw new IllegalArgumentException("Unit can not be null");
        }
        this.width = width;
        widthUnit = unit;
        markAsDirty();
        // ComponentSizeValidator.setWidthLocation(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.server.Sizeable#setWidth(java.lang.String)
     */
    @Override
    public void setWidth(String width) {
        Size size = parseStringSize(width);
        if (size != null) {
            setWidth(size.getSize(), size.getUnit());
        } else {
            setWidth(-1, Unit.PIXELS);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.server.Sizeable#setHeight(java.lang.String)
     */
    @Override
    public void setHeight(String height) {
        Size size = parseStringSize(height);
        if (size != null) {
            setHeight(size.getSize(), size.getUnit());
        } else {
            setHeight(-1, Unit.PIXELS);
        }
    }

    /*
     * Returns array with size in index 0 unit in index 1. Null or empty string
     * will produce {-1,Unit#PIXELS}
     */
    private static Size parseStringSize(String s) {
        if (s == null) {
            return null;
        }
        s = s.trim();
        if ("".equals(s)) {
            return null;
        }
        float size = 0;
        Unit unit = null;
        Matcher matcher = sizePattern.matcher(s);
        if (matcher.find()) {
            size = Float.parseFloat(matcher.group(1));
            if (size < 0) {
                size = -1;
                unit = Unit.PIXELS;
            } else {
                String symbol = matcher.group(3);
                unit = Unit.getUnitFromSymbol(symbol);
            }
        } else {
            throw new IllegalArgumentException("Invalid size argument: \"" + s
                    + "\" (should match " + sizePattern.pattern() + ")");
        }
        return new Size(size, unit);
    }

    private static class Size implements Serializable {
        float size;
        Unit unit;

        public Size(float size, Unit unit) {
            this.size = size;
            this.unit = unit;
        }

        public float getSize() {
            return size;
        }

        public Unit getUnit() {
            return unit;
        }
    }

    public interface ComponentErrorEvent extends Terminal.ErrorEvent {
    }

    public interface ComponentErrorHandler extends Serializable {
        /**
         * Handle the component error
         * 
         * @param event
         * @return True if the error has been handled False, otherwise
         */
        public boolean handleComponentError(ComponentErrorEvent event);
    }

    /**
     * Gets the error handler for the component.
     * 
     * The error handler is dispatched whenever there is an error processing the
     * data coming from the client.
     * 
     * @return
     */
    public ComponentErrorHandler getErrorHandler() {
        return errorHandler;
    }

    /**
     * Sets the error handler for the component.
     * 
     * The error handler is dispatched whenever there is an error processing the
     * data coming from the client.
     * 
     * If the error handler is not set, the application error handler is used to
     * handle the exception.
     * 
     * @param errorHandler
     *            AbstractField specific error handler
     */
    public void setErrorHandler(ComponentErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    /**
     * Handle the component error event.
     * 
     * @param error
     *            Error event to handle
     * @return True if the error has been handled False, otherwise. If the error
     *         haven't been handled by this component, it will be handled in the
     *         application error handler.
     */
    public boolean handleError(ComponentErrorEvent error) {
        if (errorHandler != null) {
            return errorHandler.handleComponentError(error);
        }
        return false;

    }

    /*
     * Actions
     */

    /**
     * Gets the {@link ActionManager} used to manage the
     * {@link ShortcutListener}s added to this {@link Field}.
     * 
     * @return the ActionManager in use
     */
    protected ActionManager getActionManager() {
        if (actionManager == null) {
            actionManager = new ActionManager();
            setActionManagerViewer();
        }
        return actionManager;
    }

    /**
     * Set a viewer for the action manager to be the parent sub window (if the
     * component is in a window) or the UI (otherwise). This is still a
     * simplification of the real case as this should be handled by the parent
     * VOverlay (on the client side) if the component is inside an VOverlay
     * component.
     */
    private void setActionManagerViewer() {
        if (actionManager != null && getUI() != null) {
            // Attached and has action manager
            Window w = findAncestor(Window.class);
            if (w != null) {
                actionManager.setViewer(w);
            } else {
                actionManager.setViewer(getUI());
            }
        }

    }

    public void addShortcutListener(ShortcutListener shortcut) {
        getActionManager().addAction(shortcut);
    }

    public void removeShortcutListener(ShortcutListener shortcut) {
        if (actionManager != null) {
            actionManager.removeAction(shortcut);
        }
    }
}