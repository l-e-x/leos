/**
 * Copyright 2016 European Commission
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
package eu.europa.ec.leos.web.support.i18n;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Scope("singleton")
@Component("languageHelper")
public class LanguageHelper {

    private static final Logger LOG = LoggerFactory.getLogger(LanguageHelper.class);

    private static final Locale FAILOVER_LOCALE = Locale.ENGLISH;

    private final DefaultLocaleInitializer defaultLocaleInitializer = new DefaultLocaleInitializer();

    @Resource(name = "applicationLanguageTags")
    private List<String> applicationLanguageTags;

    @Value("${leos.i18n.defaultLanguageTag}")
    private String defaultLanguageTag;

    private ImmutableSet<Locale> configuredLocales;

    @PostConstruct
    private void init() {
        initConfiguredLocales();
    }

    private void initConfiguredLocales() {
        LOG.trace("Initializing configured locales... [count={}]", applicationLanguageTags.size());

        // immutable set that does not allow duplicates and does not allow nulls
        ImmutableSet.Builder<Locale> builder = new ImmutableSet.Builder<>();
        int localeCount = 0;

        // initialize configured locales
        for (String languageTag : applicationLanguageTags) {
            Optional<Locale> localeOptional = resolveLocale(languageTag);
            if (localeOptional.isPresent()) {
                Locale locale = localeOptional.get();
                builder.add(locale);
                ++localeCount;
                LOG.trace("Configured locale is initialized: {}", locale.getDisplayName(Locale.ENGLISH));
            }
        }

        // fallback to default locale
        if (localeCount == 0) {
            Locale defaultLocale = getDefaultLocale();
            LOG.trace("Fallback to default locale: {}", defaultLocale.getDisplayName(Locale.ENGLISH));
            builder.add(defaultLocale);
        }

        // build the immutable set
        configuredLocales = builder.build();
        LOG.trace("Configured locales are initialized! [count={}]", configuredLocales.size());
    }

    private @Nonnull Optional<Locale> resolveLocale(@Nullable String languageTag) {
        LOG.trace("Resolving locale from language tag... [langTag={}]", languageTag);
        Optional<Locale> localeOptional = Optional.absent();

        try {
            if (StringUtils.isNotBlank(languageTag)) {
                Locale locale = Locale.forLanguageTag(languageTag);
                if ((locale != null) && StringUtils.isNotEmpty(locale.getLanguage())) {
                    localeOptional = Optional.of(locale);
                    LOG.trace("Resolved locale: {} => {}", locale.toLanguageTag(), locale.getDisplayName(Locale.ENGLISH));
                } else {
                    LOG.debug("Discarding invalid locale resolved from language tag!");
                }
            } else {
                LOG.debug("Skipping locale resolution from blank language tag!");
            }
        } catch (Exception ex) {
            LOG.warn("Unable to resolve locale!", ex);
        }

        return localeOptional;
    }

    private @Nonnull Locale getDefaultLocale() {
        Locale defaultLocale;

        try {
            defaultLocale = defaultLocaleInitializer.get();
        } catch (Exception ex) {
            LOG.warn("Defaulting to failover locale: {}", FAILOVER_LOCALE.getDisplayName(Locale.ENGLISH));
            defaultLocale = FAILOVER_LOCALE;
        }

        Validate.notNull(defaultLocale, "The default locale must not be null!");
        return defaultLocale;
    }

    public Locale getCurrentLocale() {
        // FIXME dynamically obtain the current locale
        return getDefaultLocale();
    }

    public @Nonnull Set<Locale> getConfiguredLocales() {
        Validate.notNull(configuredLocales, "The set of configured locales must not be null!");
        return configuredLocales;
    }

    public @Nonnull String getLanguageDescription(String languageCode) {
        Validate.notNull(languageCode, "The languageCode parameter must not be null!");
        return Locale.forLanguageTag(languageCode).getDisplayLanguage(getCurrentLocale());
    }

    private class DefaultLocaleInitializer extends LazyInitializer<Locale> {
        @Override
        protected Locale initialize() throws ConcurrentException {
            LOG.trace("Initializing default locale... [langTag={}]", defaultLanguageTag);
            Locale defaultLocale;

            Optional<Locale> localeOptional = resolveLocale(defaultLanguageTag);

            if (localeOptional.isPresent()) {
                defaultLocale = localeOptional.get();
            } else {
                LOG.debug("Defaulting to failover locale: {}", FAILOVER_LOCALE.getDisplayName(Locale.ENGLISH));
                defaultLocale = FAILOVER_LOCALE;
            }

            LOG.trace("Default locale is initialized: {}", defaultLocale.getDisplayName(Locale.ENGLISH));
            return defaultLocale;
        }
    }

    public String getLanguageCode(String languageDescription) {
        String languageCode = null;
        Locale currentLocale = getCurrentLocale();
        for (Locale loc : currentLocale.getAvailableLocales()) {
            if (loc.getDisplayLanguage().equals(languageDescription)) {
                languageCode = loc.getISO3Language();
                break;
            }
        }
        return languageCode;
    }
}
