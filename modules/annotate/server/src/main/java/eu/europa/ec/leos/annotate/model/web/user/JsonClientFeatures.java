/*
 * Copyright 2019 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
package eu.europa.ec.leos.annotate.model.web.user;

import eu.europa.ec.leos.annotate.Generated;

import java.util.Objects;

/**
 * Class representing hypothesis client features to be (de)activated for the user; used during user profile retrieval
 * note: currently more or less a black box for us
 */
@SuppressWarnings({"PMD.RedundantFieldInitializer", "PMD.UnnecessaryConstructor"})
public class JsonClientFeatures {

    private boolean defer_realtime_updates = false;
    private boolean flag_action = false;
    private boolean embed_cachebuster = false;
    private boolean total_shared_annotations = false;
    private boolean search_for_doi = false;
    private boolean client_oauth = false;
    private boolean api_render_user_info = false;
    private boolean orphans_tab = true;
    private boolean filter_highlights = false;
    private boolean overlay_highlighter = false;
    private boolean client_display_names = true;

    // -----------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------
    public JsonClientFeatures() {
        // default constructor required for JSON deserialisation
    }

    // -----------------------------------------------------------
    // Getters & setters
    // -----------------------------------------------------------
    public boolean isDefer_realtime_updates() {
        return defer_realtime_updates;
    }

    public void setDefer_realtime_updates(final boolean defer_realtime_updates) {
        this.defer_realtime_updates = defer_realtime_updates;
    }

    public boolean isFlag_action() {
        return flag_action;
    }

    public void setFlag_action(final boolean flag_action) {
        this.flag_action = flag_action;
    }

    public boolean isEmbed_cachebuster() {
        return embed_cachebuster;
    }

    public void setEmbed_cachebuster(final boolean embed_cachebuster) {
        this.embed_cachebuster = embed_cachebuster;
    }

    public boolean isTotal_shared_annotations() {
        return total_shared_annotations;
    }

    public void setTotal_shared_annotations(final boolean total_shared_annotations) {
        this.total_shared_annotations = total_shared_annotations;
    }

    public boolean isSearch_for_doi() {
        return search_for_doi;
    }

    public void setSearch_for_doi(final boolean search_for_doi) {
        this.search_for_doi = search_for_doi;
    }

    public boolean isClient_oauth() {
        return client_oauth;
    }

    public void setClient_oauth(final boolean client_oauth) {
        this.client_oauth = client_oauth;
    }

    public boolean isApi_render_user_info() {
        return api_render_user_info;
    }

    public void setApi_render_user_info(final boolean api_render_user_info) {
        this.api_render_user_info = api_render_user_info;
    }

    public boolean isOrphans_tab() {
        return orphans_tab;
    }

    public void setOrphans_tab(final boolean orphans_tab) {
        this.orphans_tab = orphans_tab;
    }

    public boolean isFilter_highlights() {
        return filter_highlights;
    }

    public void setFilter_highlights(final boolean filter_highlights) {
        this.filter_highlights = filter_highlights;
    }

    public boolean isOverlay_highlighter() {
        return overlay_highlighter;
    }

    public void setOverlay_highlighter(final boolean overlay_highlighter) {
        this.overlay_highlighter = overlay_highlighter;
    }

    public boolean isClient_display_names() {
        return client_display_names;
    }

    public void setClient_display_names(final boolean client_display_names) {
        this.client_display_names = client_display_names;
    }

    // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(defer_realtime_updates, flag_action, embed_cachebuster,
                total_shared_annotations, search_for_doi, client_oauth, api_render_user_info,
                orphans_tab, filter_highlights, overlay_highlighter, client_display_names);
    }

    @Generated
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final JsonClientFeatures other = (JsonClientFeatures) obj;
        return Objects.equals(this.defer_realtime_updates, other.defer_realtime_updates) &&
                Objects.equals(this.flag_action, other.flag_action) &&
                Objects.equals(this.embed_cachebuster, other.embed_cachebuster) &&
                Objects.equals(this.total_shared_annotations, other.total_shared_annotations) &&
                Objects.equals(this.search_for_doi, other.search_for_doi) &&
                Objects.equals(this.client_oauth, other.client_oauth) &&
                Objects.equals(this.api_render_user_info, other.api_render_user_info) &&
                Objects.equals(this.orphans_tab, other.orphans_tab) &&
                Objects.equals(this.filter_highlights, other.filter_highlights) &&
                Objects.equals(this.overlay_highlighter, other.overlay_highlighter) &&
                Objects.equals(this.client_display_names, other.client_display_names);
    }
}
