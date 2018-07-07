====
    Copyright 2017 European Commission

    Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
    You may not use this work except in compliance with the Licence.
    You may obtain a copy of the Licence at:

        https://joinup.ec.europa.eu/software/page/eupl

    Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the Licence for the specific language governing permissions and limitations under the Licence.
====

1. A document styling is composed from main files (ie XML, EDITOR, COVERPAGE) using modules, utils and shared styles.
2. A main file is a mixin which contain all the modules required with appropriate scoping.
3. A module caters to a specific displayed element and divided into parts each pertaining to different representation (xml, editor or showblock).
4. In utils and modules, placeholders are preferred. Mixins are used when parameters are required for style construction.
5. The utils contains common variables, place holders or mixins.
