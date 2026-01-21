// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.utils;

import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import java.net.URL;

public final class IconHelper{
    private IconHelper(){
        super();
    }

    public static ImageIcon getIcon(@NonNls String location){
        final Class<IconHelper> thisClass = IconHelper.class;
        final URL resource = thisClass.getResource(location);
        return new ImageIcon(resource);
    }
}
