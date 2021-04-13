/*
 * Copyright 2000-2006 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jetbrains.communicator.idea;

import jetbrains.communicator.core.users.User;

import java.util.Date;

/**
 * @author Kir
 */
public interface ConsoleMessage extends ConsoleUtil.Printer {
  User getUser();
  String getUsername();
  Date getWhen();

  /** Message, Code Pointer, Stacktrace*/
  String getTitle();
}
