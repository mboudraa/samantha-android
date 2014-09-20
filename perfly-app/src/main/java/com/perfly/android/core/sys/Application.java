/*
 * Copyright (c) 2014 Mounir Boudraa
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.perfly.android.core.sys;

import android.graphics.drawable.Drawable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.perfly.android.core.json.DrawableDeserializer;
import com.perfly.android.core.json.DrawableSerializer;

public class Application {

    @JsonProperty
    public final String label;

    @JsonSerialize(using = DrawableSerializer.class)
    @JsonDeserialize(using = DrawableDeserializer.class)
    @JsonProperty
    public final Drawable logo;

    @JsonProperty
    public final String version;

    @JsonProperty
    public final int uid;

    @JsonProperty
    public final boolean debuggable;

    @JsonProperty
    public final String packageName;

    @JsonCreator
    public Application(int uid, String label, Drawable logo, String version, String packageName, boolean debuggable) {
        this.label = label;
        this.logo = logo;
        this.version = version;
        this.uid = uid;
        this.debuggable = debuggable;
        this.packageName = packageName;
    }
}
