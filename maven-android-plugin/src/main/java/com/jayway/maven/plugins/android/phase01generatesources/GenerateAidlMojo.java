/*
 * Copyright (C) 2007-2008 JVending Masa
 * Copyright (C) 2009 Jayway AB
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
package com.jayway.maven.plugins.android.phase01generatesources;

import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.CommandExecutor;
import com.jayway.maven.plugins.android.ExecutionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates java files based on aidl files.<br/>
 * If the configuration parameter <code>deleteConflictingFiles</code> is <code>true</code> (which it is by default), this
 * goal has the following side-effect:
 * <ul>
 * <li>deletes any <code>.java</code> files with the same name as an <code>.aidl</code> file found in the source
 * directory.</li>
 * </ul>
 * @goal generateAidl
 * @requiresProject true
 * @author hugo.josefson@jayway.com
 */
public class GenerateAidlMojo extends AbstractAndroidMojo {

    public void execute() throws MojoExecutionException, MojoFailureException {
        final String sourceDirectory = project.getBuild().getSourceDirectory();

        String[] files = findFilesInDirectory(sourceDirectory, "**/*.aidl");
        getLog().info("ANDROID-904-002: Found aidl files: Count = " + files.length);
        if (files.length == 0) {
            return;
        }

        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger(this.getLog());

        File generatedSourcesDirectory = new File(project.getBuild().getDirectory() + File.separator + "generated-sources" + File.separator + "aidl");
        generatedSourcesDirectory.mkdirs();

        int numberOfFilesDeleted = 0;
        for (String relativeAidlFileName : files) {
            List<String> commands = new ArrayList<String>();
            // TODO: DON'T use System.getenv for this! Use proper plugin configuration parameters,
            // TODO: (which may pull from environment/ANDROID_SDK for their default values.)
            if (System.getenv().get("ANDROID_SDK") != null) {
                commands.add("-p" + System.getenv().get("ANDROID_SDK") + "/tools/lib/framework.aidl");
            }
            File targetDirectory = new File(generatedSourcesDirectory, new File(relativeAidlFileName).getParent());
            targetDirectory.mkdirs();

            final String shortAidlFileName         = new File(relativeAidlFileName).getName();
            final String shortJavaFileName         = shortAidlFileName.substring(0, shortAidlFileName.lastIndexOf("."))       + ".java";
            final String relativeJavaFileName      = relativeAidlFileName.substring(0, relativeAidlFileName.lastIndexOf(".")) + ".java";
            final File   aidlFileInSourceDirectory = new File(sourceDirectory, relativeAidlFileName);

            if (deleteConflictingFiles) {
                final File javaFileInSourceDirectory = new File(sourceDirectory, relativeJavaFileName);

                if (javaFileInSourceDirectory.exists()) {
                    final boolean successfullyDeleted = javaFileInSourceDirectory.delete();
                    if (successfullyDeleted) {
                        numberOfFilesDeleted++;
                    } else {
                        throw new MojoExecutionException("Failed to delete \"" + javaFileInSourceDirectory + "\"");
                    }
                }
            }

            commands.add("-I" + sourceDirectory);
            commands.add(aidlFileInSourceDirectory.getAbsolutePath());
            commands.add(new File(targetDirectory , shortJavaFileName).getAbsolutePath());
            try {
                executor.executeCommand("aidl", commands, project.getBasedir(), false);
            } catch (ExecutionException e) {
                throw new MojoExecutionException("", e);
            }
        }

        if (numberOfFilesDeleted > 0){
            getLog().info("Deleted " + numberOfFilesDeleted + " conflicting aidl-generated *.java file(s) in source directory. If you use Eclipse, please Refresh (F5) the project to regain them.");
        }

        project.addCompileSourceRoot(generatedSourcesDirectory.getPath());
        
    }
}
