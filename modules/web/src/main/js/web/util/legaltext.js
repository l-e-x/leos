/*
 * Copyright 2015 European Commission
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
/**
 * Show the actions available on an article
 * 
 * @param element
 *            the article tag
 */
function leg_hideArticleActions(articleId) {
    var el = document.getElementById('art_txt_' + articleId);
    el.className = 'leos-article-content';
    
    el = document.getElementById('all_actions_' + articleId);
    el.className = 'leos-actions';
    (Array.prototype.slice.call(el.childNodes)).forEach(function(elChild){
        elChild.className ='leos-action-inactive';
    });
}

function leg_hidePreambleActions(preambleId) {
    var el = document.getElementById('pre_text_preamble_'+preambleId);
    el.className = 'leos-preamble-content ';
    
    el = document.getElementById('all_actions_'+preambleId);
    el.className = 'leos-actions';
    (Array.prototype.slice.call(el.childNodes)).forEach(function(elChild){
        elChild.className ='leos-action-inactive';
    });
}

function leg_showArticleActions(articleId) {
    var el = document.getElementById('art_txt_' + articleId);
    el.className = el.className + ' leos-article-content-active';

    leg_setIconsPerLocks(articleId);
}

function leg_showPreambleActions(preambleId) {
    var el = document.getElementById('pre_text_preamble_'+preambleId);
    el.className = el.className + ' leos-preamble-content-active';
    
    leg_setIconsPerLocks(preambleId);
}

function leg_setIconsPerLocks(elementId) {
    setAllActionsEnabled(elementId);
    for(var iIndex=0;iIndex < documentLocks.length;iIndex++){
        var lock= documentLocks[iIndex];
        if(lock.lockLevel === 'DOCUMENT_LOCK' || lock.elementId === elementId){
            setLockIconEnabled(elementId);
            var el = document.getElementById('lock_icon_' + elementId);
												el.title =  (lock.lockLevel === 'DOCUMENT_LOCK') ?
                 'Complete document is locked by user ' + lock.userName:
                 'Element is locked by user ' + lock.userName;
            break;
        }
        else if(lock.lockLevel === 'ELEMENT_LOCK' && lock.elementId !== elementId){
            setEditArticleEnabled(elementId);
        }
    }
}

function setAllActionsEnabled(elementId){
    var el = document.getElementById('all_actions_' + elementId);
    el.className = el.className  + ' leos-actions-active';

    var childArray= Array.prototype.slice.call(el.childNodes);
    childArray.forEach(function(elChild){
        elChild.className = (elChild.id === ('lock_icon_' + elementId)) ? 'leos-action-inactive' : 'leos-action-active';
    });
}

function setEditArticleEnabled(elementId) {
    var el = document.getElementById('all_actions_' + elementId);
    el.className = el.className + ' leos-actions-active';

    var childArray = Array.prototype.slice.call(el.childNodes);
    childArray.forEach(function(elChild) {
        elChild.className = (elChild.id === ('edit_icon_' + elementId)) ? 'leos-action-active' : 'leos-action-inactive';
    });
}

function setLockIconEnabled(elementId){
    var el = document.getElementById('all_actions_' + elementId);
    el.className = el.className  + ' leos-actions-active';

    var childArray= Array.prototype.slice.call(el.childNodes);
    childArray.forEach(function(elChild){
        elChild.className = (elChild.id===('lock_icon_' + elementId)) ?'leos-action-active':'leos-action-inactive';
    });
}

function leg_getArticleIdForElement(element) {
    var elId = element.id;
    var articleId = elId.substring(elId.indexOf('articleId_') + 'articleId_'.length);
    return articleId;
}


function leg_getPreambleId(element) {
    var elId = element.id;
    var preambleId = elId.substring(elId.indexOf('preamble_') + 'preamble_'.length);
    return preambleId;
}

function leg_scrollIntoView(eId) {
   var e = document.getElementById(eId);
   if (e && e.scrollIntoView) {
       e.scrollIntoView();
   }
}
//The javascript actions itself are implemented in VAADIN
