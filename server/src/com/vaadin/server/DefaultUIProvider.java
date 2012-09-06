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

package com.vaadin.server;

import com.vaadin.ui.UI;

public class DefaultUIProvider extends AbstractUIProvider {

    @Override
    public Class<? extends UI> getUIClass(VaadinSession application,
            WrappedRequest request) {
        Object uiClassNameObj = application.getConfiguration()
                .getInitParameters().getProperty(VaadinSession.UI_PARAMETER);

        if (uiClassNameObj instanceof String) {
            String uiClassName = uiClassNameObj.toString();

            ClassLoader classLoader = request.getVaadinService()
                    .getClassLoader();
            if (classLoader == null) {
                classLoader = getClass().getClassLoader();
            }
            try {
                Class<? extends UI> uiClass = Class.forName(uiClassName, true,
                        classLoader).asSubclass(UI.class);

                return uiClass;
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Could not find UI class", e);
            }
        }

        return null;
    }
}