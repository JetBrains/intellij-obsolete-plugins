// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.actions;

import com.intellij.guice.constants.GuiceClasses;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NonNls;

import java.io.IOException;
import java.util.Properties;

public class GuiceProviderBuilder{
    private String className = null;
    private String providedClassName = null;

    public void setClassName(String className){
        this.className = className;
    }

    public void setProvidedClassName(String providedClassName){
        this.providedClassName = providedClassName;
    }

    public String buildProviderClass(Project project) throws IOException{
        final @NonNls StringBuilder out = new StringBuilder(1024);
        final FileTemplateManager templateManager = FileTemplateManager.getInstance(project);
        FileTemplate headerTemplate;
        try{
            headerTemplate = templateManager.getDefaultTemplate(FileTemplateManager.FILE_HEADER_TEMPLATE_NAME);
        } catch(Exception e){
            headerTemplate = null;
        }
        final Properties defaultProperties = templateManager.getDefaultProperties();
        final @NonNls Properties properties = new Properties(defaultProperties);
        properties.setProperty("PACKAGE_NAME", "");
        properties.setProperty("NAME", className);
        if(headerTemplate != null){
            final @NonNls String headerText = headerTemplate.getText(properties);

            final String cleanedText = headerText.replace("public file header " + className + " { }", "");
            out.append(cleanedText);
        }
        out.append('\n');
        out.append("public class " + className);
        out.append(" implements ");
        out.append(GuiceClasses.PROVIDER);
        out.append('<' + providedClassName + '>');
        out.append('\n');

        out.append('{');
        out.append("public " + providedClassName + " get(){\n");
        out.append("//TODO: add provider logic here\n");
        out.append("return null;\n");
        out.append("}\n}\n");
        return out.toString();
    }
}