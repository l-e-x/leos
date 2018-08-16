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

define(function leosBase64ImageDialog(require) {
    "use strict";
    
    var CKEDITOR = require("promise!ckEditor");
    
    var dialogDefinition = {
        dialogName: "leosBase64ImageDialog"
    };
    dialogDefinition.initializeDialog = function initializeDialog(editor) {
        var t = null,
            selectedImg = null,
            orgWidth = null, orgHeight = null,
            imgPreview = null, imgScal = 1, lock = true, MAX_IMAGE_SRC_LENGTH = 1048575, MAX_IMAGE_SIZE_IN_KB = 700;
        // More or less a base64 is calculated Math.round(MAX_IMAGE_SRC_LENGTH*3/4) giving 767kb of image size, we'll allow 700kb.
        
        function validateImageSize(src) {
            return src.length < MAX_IMAGE_SRC_LENGTH;
        }
        
        /* Load preview image */
        function imagePreviewLoad(s) {
            
            if (validateImageSize(s)) {
                
                /* no preview */
                if (typeof(s) != "string" || !s) {
                    imgPreview.getElement().setHtml("");
                    return;
                }
                
                /* Create image */
                var i = new Image();
                
                /* Display loading text in preview element */
                imgPreview.getElement().setHtml(editor.lang.base64image.loading);
                
                /* When image is loaded */
                i.onload = function () {
                    
                    /* Remove preview */
                    imgPreview.getElement().setHtml("");
                    
                    /* Set attributes */
                    if (orgWidth == null || orgHeight == null) {
                        t.setValueOf("tab-source", "width", this.width);
                        t.setValueOf("tab-source", "height", this.height);
                        imgScal = 1;
                        if (this.height > 0 && this.width > 0) imgScal = this.width / this.height;
                        if (imgScal <= 0) imgScal = 1;
                    } else {
                        orgWidth = null;
                        orgHeight = null;
                    }
                    this.id = editor.id + "previewimage";
                    this.setAttribute("style", "max-width:400px;max-height:100px;");
                    this.setAttribute("alt", "");
                    
                    /* Insert preview image */
                    try {
                        var p = imgPreview.getElement().$;
                        if (p) p.appendChild(this);
                    } catch (e) {
                    }
                    CKEDITOR.dialog.getCurrent().enableButton("ok");
                };
                
                /* Error Function */
                i.onerror = function () {
                    imgPreview.getElement().setHtml("");
                };
                i.onabort = function () {
                    imgPreview.getElement().setHtml("");
                };
                
                /* Load image */
                i.src = s;
            } else {
                imgPreview.getElement().setHtml("<div style=\"text-align:center;color:red;\">" + editor.lang.base64image.sizeNotValid + MAX_IMAGE_SIZE_IN_KB + "kb<div>");
                CKEDITOR.dialog.getCurrent().disableButton("ok");
            }
        }
        
        /* Change input values and preview image */
        function imagePreview(src) {
            /* Remove preview */
            imgPreview.getElement().setHtml("");
            /* Read file and load preview */
            var fileI = t.getContentElement("tab-source", "file");
            var n = null;
            try {
                n = fileI.getInputElement().$;
            } catch (e) {
                n = null;
            }
            if (n && "files" in n && n.files && n.files.length > 0 && n.files[0]) {
                if ("type" in n.files[0] && !n.files[0].type.match("image.*")) return;
                if (!FileReader) return;
                imgPreview.getElement().setHtml(editor.lang.base64image.loading);
                var fr = new FileReader();
                fr.onload = (function (f) {
                    return function (e) {
                        imgPreview.getElement().setHtml("");
                        imagePreviewLoad(e.target.result);
                    };
                })(n.files[0]);
                fr.onerror = function () {
                    imgPreview.getElement().setHtml("");
                };
                fr.onabort = function () {
                    imgPreview.getElement().setHtml("");
                };
                fr.readAsDataURL(n.files[0]);
            }
        }
        
        /* Calculate image dimensions */
        function getImageDimensions() {
            var o = {
                "w": t.getContentElement("tab-source", "width").getValue(),
                "h": t.getContentElement("tab-source", "height").getValue(),
                "uw": "px",
                "uh": "px"
            };
            if (o.w.indexOf("%") >= 0) o.uw = "%";
            if (o.h.indexOf("%") >= 0) o.uh = "%";
            o.w = parseInt(o.w, 10);
            o.h = parseInt(o.h, 10);
            if (isNaN(o.w)) o.w = 0;
            if (isNaN(o.h)) o.h = 0;
            return o;
        }
        
        /* Set image dimensions */
        function imageDimensions(src) {
            var o = getImageDimensions();
            var u = "px";
            if (src == "width") {
                if (o.uw == "%") u = "%";
                o.h = Math.round(o.w / imgScal);
            } else {
                if (o.uh == "%") u = "%";
                o.w = Math.round(o.h * imgScal);
            }
            if (u == "%") {
                o.w += "%";
                o.h += "%";
            }
            t.getContentElement("tab-source", "width").setValue(o.w),
                t.getContentElement("tab-source", "height").setValue(o.h)
        }
        
        /* Set integer Value */
        function integerValue(elem) {
            var v = elem.getValue(), u = "";
            if (v.indexOf("%") >= 0) u = "%";
            v = parseInt(v, 10);
            if (isNaN(v)) v = 0;
            elem.setValue(v + u);
        }
        
        /* Dialog */
        return {
            title: editor.lang.common.image,
            minWidth: 310,
            minHeight: 100,
            onLoad: function () {
                /* Get image preview element */
                imgPreview = this.getContentElement("tab-source", "preview");
                
                /* Constrain proportions or not */
                this.getContentElement("tab-source", "lock").getInputElement().on("click", function () {
                    if (this.getValue()) lock = true; else lock = false;
                    if (lock) imageDimensions("width");
                }, this.getContentElement("tab-source", "lock"));
                
                /* Change Attributes Events  */
                this.getContentElement("tab-source", "width").getInputElement().on("keyup", function () {
                    if (lock) imageDimensions("width");
                });
                this.getContentElement("tab-source", "height").getInputElement().on("keyup", function () {
                    if (lock) imageDimensions("height");
                });
                
            },
            onShow: function () {
                
                /* Remove preview */
                imgPreview.getElement().setHtml("");
                
                t = this, orgWidth = null, orgHeight = null, imgScal = 1, lock = true;
                
                /* selected image or null */
                selectedImg = editor.getSelection();
                if (selectedImg) selectedImg = selectedImg.getSelectedElement();
                if (!selectedImg || selectedImg.getName() !== "img") selectedImg = null;
                
                /* Set input values */
                t.setValueOf("tab-source", "lock", lock);
                
                if (selectedImg) {
                    
                    /* Set input values from selected image */
                    if (typeof(selectedImg.getAttribute("width")) == "string") orgWidth = selectedImg.getAttribute("width");
                    if (typeof(selectedImg.getAttribute("height")) == "string") orgHeight = selectedImg.getAttribute("height");
                    if ((orgWidth == null || orgHeight == null) && selectedImg.$) {
                        orgWidth = selectedImg.$.width;
                        orgHeight = selectedImg.$.height;
                    }
                    if (orgWidth != null && orgHeight != null) {
                        t.setValueOf("tab-source", "width", orgWidth);
                        t.setValueOf("tab-source", "height", orgHeight);
                        orgWidth = parseInt(orgWidth, 10);
                        orgHeight = parseInt(orgHeight, 10);
                        imgScal = 1;
                        if (!isNaN(orgWidth) && !isNaN(orgHeight) && orgHeight > 0 && orgWidth > 0) imgScal = orgWidth / orgHeight;
                        if (imgScal <= 0) imgScal = 1;
                    }
                    
                    if (typeof(selectedImg.getAttribute("src")) == "string") {
                        if (selectedImg.getAttribute("src").indexOf("data:") === 0) {
                            imagePreview("base64");
                            imagePreviewLoad(selectedImg.getAttribute("src"));
                        } else {
                            t.setValueOf("tab-source", "url", selectedImg.getAttribute("src"));
                        }
                    }
                    if (typeof(selectedImg.getAttribute("alt")) == "string") t.setValueOf("tab-source", "alt", selectedImg.getAttribute("alt"));
                    
                    t.selectPage("tab-source");
                }
                
            },
            onOk: function () {
                
                /* Get image source */
                var src = "";
                try {
                    src = CKEDITOR.document.getById(editor.id + "previewimage").$.src;
                } catch (e) {
                    src = "";
                }
                if (typeof(src) != "string" || src == null || src === "") return;
                
                /* selected image or new image */
                if (selectedImg) var newImg = selectedImg; else var newImg = editor.document.createElement("img");
                newImg.setAttribute("src", src);
                src = null;
                
                /* Set attributes */
                newImg.setAttribute("alt", t.getValueOf("tab-source", "alt").replace(/^\s+/, "").replace(/\s+$/, ""));
                var attr = {
                    "width": ["width", "width:#;", "integer", 1],
                    "height": ["height", "height:#;", "integer", 1],
                }, css = [], value, cssvalue, attrvalue, k;
                var unit = "px";
                for (k in attr) {
                    
                    value = t.getValueOf("tab-source", k);
                    attrvalue = value;
                    cssvalue = value;
                    
                    if (attr[k][2] == "integer") {
                        if (value.indexOf("%") >= 0) unit = "%";
                        value = parseInt(value, 10);
                        if (isNaN(value)) value = null; else if (value < attr[k][3]) value = null;
                        if (value != null) {
                            if (unit == "%") {
                                attrvalue = value + "%";
                                cssvalue = value + "%";
                            } else {
                                attrvalue = value;
                                cssvalue = value + "px";
                            }
                        }
                    }
                    
                    if (value != null) {
                        newImg.setAttribute(attr[k][0], attrvalue);
                        css.push(attr[k][1].replace(/#/g, cssvalue));
                    }
                    
                }
                if (css.length > 0) newImg.setAttribute("style", css.join(""));
                
                /* Insert new image */
                if (!selectedImg) editor.insertElement(newImg);
                
                /* Resize image */
                if (editor.plugins.imageresize) editor.plugins.imageresize.resize(editor, newImg, 800, 800);
                
            },
            
            /* Dialog form */
            contents: [
                {
                    id: "tab-source",
                    label: editor.lang.common.generalTab,
                    elements: [
                        {
                            type: "text",
                            id: "alt",
                            label: editor.lang.base64image.alt
                        },
                        {
                            type: "hbox",
                            widths: ["70px"],
                            children: [
                                {
                                    type: "file",
                                    id: "file",
                                    label: "",
                                    onChange: function () {
                                        imagePreview("file");
                                    }
                                }
                            ]
                        },
                        {
                            type: "html",
                            id: "preview",
                            html: new CKEDITOR.template("<div style=\"text-align:center;\"></div>").output()
                        },
                        {
                            type: 'hbox',
                            widths: ["15%", "15%", "70%"],
                            children: [
                                {
                                    type: "text",
                                    width: "45px",
                                    id: "width",
                                    label: editor.lang.common.width
                                },
                                {
                                    type: "text",
                                    width: "45px",
                                    id: "height",
                                    label: editor.lang.common.height
                                },
                                {
                                    type: "checkbox",
                                    id: "lock",
                                    label: editor.lang.base64image.lockRatio,
                                    style: "margin-top:18px;"
                                }
                            ]
                        }
                    ]
                }
            ]
        };
    };
    return dialogDefinition;
});
