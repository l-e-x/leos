/*
 * Copyright 2018 European Commission
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

/**
 * Class representing hypothesis client features to be (de)activated for the user; used during user profile retrieval
 * note: currently more or less a black box for us
 */
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
    }

    // -----------------------------------------------------------
    // Getters & setters
    // -----------------------------------------------------------
    public boolean isDefer_realtime_updates() {
        return defer_realtime_updates;
    }

    public void setDefer_realtime_updates(boolean defer_realtime_updates) {
        this.defer_realtime_updates = defer_realtime_updates;
    }

    public boolean isFlag_action() {
        return flag_action;
    }

    public void setFlag_action(boolean flag_action) {
        this.flag_action = flag_action;
    }

    public boolean isEmbed_cachebuster() {
        return embed_cachebuster;
    }

    public void setEmbed_cachebuster(boolean embed_cachebuster) {
        this.embed_cachebuster = embed_cachebuster;
    }

    public boolean isTotal_shared_annotations() {
        return total_shared_annotations;
    }

    public void setTotal_shared_annotations(boolean total_shared_annotations) {
        this.total_shared_annotations = total_shared_annotations;
    }

    public boolean isSearch_for_doi() {
        return search_for_doi;
    }

    public void setSearch_for_doi(boolean search_for_doi) {
        this.search_for_doi = search_for_doi;
    }

    public boolean isClient_oauth() {
        return client_oauth;
    }

    public void setClient_oauth(boolean client_oauth) {
        this.client_oauth = client_oauth;
    }

    public boolean isApi_render_user_info() {
        return api_render_user_info;
    }

    public void setApi_render_user_info(boolean api_render_user_info) {
        this.api_render_user_info = api_render_user_info;
    }

    public boolean isOrphans_tab() {
        return orphans_tab;
    }

    public void setOrphans_tab(boolean orphans_tab) {
        this.orphans_tab = orphans_tab;
    }

    public boolean isFilter_highlights() {
        return filter_highlights;
    }

    public void setFilter_highlights(boolean filter_highlights) {
        this.filter_highlights = filter_highlights;
    }

    public boolean isOverlay_highlighter() {
        return overlay_highlighter;
    }

    public void setOverlay_highlighter(boolean overlay_highlighter) {
        this.overlay_highlighter = overlay_highlighter;
    }

    public boolean isClient_display_names() {
        return client_display_names;
    }

    public void setClient_display_names(boolean client_display_names) {
        this.client_display_names = client_display_names;
    }
}
