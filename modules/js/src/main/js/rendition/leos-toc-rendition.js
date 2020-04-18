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
var showToc = true;

if (typeof jQuery == 'undefined') {
    document.write('<script src="js/jquery.js"></' + 'script>');
    document.write('<script src="js/jqtree.js"></' + 'script>');
} else if (jQuery.fn.jquery >= "1.9") {
    _log("Compatible jquery v:" + jQuery.fn.jquery + " found on the host. Compatible versions: 1.9+, 2.x or 3.x");
    document.write('<script src="js/jqtree.js"></' + 'script>');
} else {
    showToc = false;
    _log("Hiding the TOC! NOT compatible jquery v:" + jQuery.fn.jquery + " found on the host. Required: 1.9+, 2.x or 3.x");
}

function buildTree(tree, toc_data) {
    tree.tree({
        data: toc_data,
        autoOpen: true
    });
    
    tree.on(
        'tree.click',
        function (e) {
            var selected_node = e.node;
            _log('selected_node: ' + selected_node.name + ", links to :" + selected_node.href);
            $(".renditionAkomaNtosoContent").scrollTop(0); // Need to come back to div top before using element offset
       		$(".renditionAkomaNtosoContent").scrollTop($(selected_node.href).offset().top);
        }
    );
}

function _log(msg){
    window.console && console.log(msg);
}

//Wait until the dom is completely loaded to be able tp access the div
document.addEventListener("DOMContentLoaded", function (event) {
    if (showToc) {
        var treeContainer = $('#treeContainer');
        buildTree(treeContainer, toc_data); // toc_data is declared in the template with json data
    } else {
        var notCompliantDiv = document.getElementById('notCompliantDiv');
        notCompliantDiv.style.display = "block";
        
        var treeContainer = document.getElementById('treeContainer');
        treeContainer.style.display = "none";
    }
});


