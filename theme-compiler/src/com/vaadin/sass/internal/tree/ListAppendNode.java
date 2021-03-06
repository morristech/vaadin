/*
 * Copyright 2000-2013 Vaadin Ltd.
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
package com.vaadin.sass.internal.tree;

import java.util.ArrayList;

public class ListAppendNode extends ListModifyNode {

    public ListAppendNode(String variable, String list, String append,
            String separator) {
        this.variable = variable;
        checkSeparator(separator, list);
        populateList(list, append);
    }

    @Override
    protected void modifyList(ArrayList<String> newList) {
        newList.addAll(modify);
    }

}
