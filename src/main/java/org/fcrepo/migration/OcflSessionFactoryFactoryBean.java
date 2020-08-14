/*
 * Copyright 2019 DuraSpace, Inc.
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

package org.fcrepo.migration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.wisc.library.ocfl.core.OcflRepositoryBuilder;
import edu.wisc.library.ocfl.core.extension.storage.layout.config.HashedTruncatedNTupleConfig;
import edu.wisc.library.ocfl.core.path.mapper.LogicalPathMappers;
import edu.wisc.library.ocfl.core.storage.filesystem.FileSystemOcflStorage;
import org.apache.commons.lang3.SystemUtils;
import org.fcrepo.migration.handlers.ocfl.VanillaOcflObjectSessionFactory;
import org.fcrepo.storage.ocfl.CommitType;
import org.fcrepo.storage.ocfl.DefaultOcflObjectSessionFactory;
import org.fcrepo.storage.ocfl.OcflObjectSessionFactory;
import org.springframework.beans.factory.FactoryBean;

import java.nio.file.Path;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;

/**
 * Spring FactoryBean for easy OcflObjectSessionFactory creation.
 *
 * @author pwinckles
 */
public class OcflSessionFactoryFactoryBean implements FactoryBean<OcflObjectSessionFactory> {

    private final Path ocflRoot;
    private final Path stagingDir;
    private final MigrationType migrationType;
    private final String user;
    private final String userUri;

    /**
     * @param ocflRoot OCFL storage root
     * @param stagingDir OCFL staging dir
     * @param migrationType migration type
     * @param user user to add to OCFL versions
     * @param userUri user's address
     */
    public OcflSessionFactoryFactoryBean(final Path ocflRoot,
                                         final Path stagingDir,
                                         final MigrationType migrationType,
                                         final String user,
                                         final String userUri) {
        this.ocflRoot = ocflRoot;
        this.stagingDir = stagingDir;
        this.migrationType = migrationType;
        this.user = user;
        this.userUri = userUri;
    }

    @Override
    public OcflObjectSessionFactory getObject() {
        final var logicalPathMapper = SystemUtils.IS_OS_WINDOWS ?
                LogicalPathMappers.percentEncodingWindowsMapper() : LogicalPathMappers.percentEncodingLinuxMapper();

        final var ocflRepo =  new OcflRepositoryBuilder()
                .layoutConfig(new HashedTruncatedNTupleConfig())
                .logicalPathMapper(logicalPathMapper)
                .storage(FileSystemOcflStorage.builder().repositoryRoot(ocflRoot).build())
                .workDir(stagingDir)
                .buildMutable();

        if (migrationType == MigrationType.F6_OCFL) {
            final var objectMapper = new ObjectMapper()
                    .configure(WRITE_DATES_AS_TIMESTAMPS, false)
                    .registerModule(new JavaTimeModule())
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL);

            return new DefaultOcflObjectSessionFactory(ocflRepo, stagingDir, objectMapper, CommitType.NEW_VERSION,
                    "Generated by Fedora 3 to Fedora 6 migration", user, userUri);
        } else {
            return new VanillaOcflObjectSessionFactory(ocflRepo, stagingDir,
                    "Generated by Fedora 3 to Fedora 6 migration", user, userUri);
        }
    }

    @Override
    public Class<?> getObjectType() {
        return OcflObjectSessionFactory.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

}
