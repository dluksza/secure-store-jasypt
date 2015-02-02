// Copyright (C) 2015 CollabNet
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.collabnet.gerrit;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.gerrit.common.FileUtil;
import com.google.gerrit.server.config.SitePaths;
import com.google.gerrit.server.securestore.SecureStore;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.jgit.internal.storage.file.LockFile;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.salt.StringFixedSaltGenerator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class SecureStoreJasypt extends SecureStore {
  private final FileBasedConfig sec;
  private final StandardPBEStringEncryptor encryptor;

  @Inject
  SecureStoreJasypt(SitePaths site) {
    File secureConfig = new File(site.etc_dir, "secure.config");
    if (!secureConfig.exists()) {
      try {
        if (!site.etc_dir.exists()) {
          Files.createDirectories(site.etc_dir.toPath());
        }
        Files.createFile(secureConfig.toPath());
      } catch (IOException e) {
        throw new RuntimeException("Cannot create: "
            + secureConfig.getAbsolutePath(), e);
      }
    }
    sec = new FileBasedConfig(secureConfig, FS.DETECTED);
    encryptor = new StandardPBEStringEncryptor();
    encryptor.setPassword("8f(_^?2|#D/z->^4(>~(/y|3{7?^<J");
    encryptor.setAlgorithm("PBEWithMD5AndTripleDES");
    encryptor.setSaltGenerator(new StringFixedSaltGenerator("Kucudra4"));

    try {
      sec.load();
    } catch (Exception e) {
      throw new RuntimeException("Cannot load secure.config", e);
    }
  }

  @Override
  public String[] getList(String section, String subsection, String name) {
    String[] stringList = sec.getStringList(section, subsection, name);
    if (stringList != null) {
      List<String> decrypted = new ArrayList<>(stringList.length);
      for (String element : stringList) {
        if (!Strings.isNullOrEmpty(element)) {
          decrypted.add(encryptor.decrypt(element));
        }
      }
      return decrypted.toArray(new String[decrypted.size()]);
    }
    return null;
  }

  @Override
  public void setList(String section, String subsection, String name,
      List<String> values) {
    List<String> encrypted = new ArrayList<>(values.size());
    for (String value : values) {
      encrypted.add(encryptor.encrypt(value));
    }
    if (!values.isEmpty()) {
      sec.setStringList(section, subsection, name, encrypted);
    } else {
      sec.unset(section, subsection, name);
    }
    save();
  }

  @Override
  public void unset(String section, String subsection, String name) {
    sec.unset(section, subsection, name);
    save();
  }

  @Override
  public Iterable<EntryKey> list() {
    ImmutableSet.Builder<EntryKey> result = ImmutableSet.builder();
    for (String section : sec.getSections()) {
      for (String subsection : sec.getSubsections(section)) {
        for (String name : sec.getNames(section, subsection)) {
          result.add(new EntryKey(section, subsection, name));
        }
      }
      for (String name : sec.getNames(section)) {
        result.add(new EntryKey(section, null, name));
      }
    }
    return result.build();
  }

  private void save() {
    try {
      saveSecure(sec);
    } catch (IOException e) {
      throw new RuntimeException("Cannot save secure.config", e);
    }
  }

  private static void saveSecure(final FileBasedConfig sec) throws IOException {
    if (FileUtil.modified(sec)) {
      final byte[] out = Constants.encode(sec.toText());
      final File path = sec.getFile();
      final LockFile lf = new LockFile(path, FS.DETECTED);
      if (!lf.lock()) {
        throw new IOException("Cannot lock " + path);
      }
      try {
        FileUtil.chmod(0600, new File(path.getParentFile(), path.getName()
            + ".lock"));
        lf.write(out);
        if (!lf.commit()) {
          throw new IOException("Cannot commit write to " + path);
        }
      } finally {
        lf.unlock();
      }
    }
  }
}
