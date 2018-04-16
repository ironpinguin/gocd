/*
 * Copyright 2018 ThoughtWorks, Inc.
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

describe('Artifact Stores Widget', () => {

  const $             = require("jquery");
  const m             = require("mithril");
  const simulateEvent = require('simulate-event');
  const Modal         = require('views/shared/new_modal');

  require('jasmine-jquery');
  require('jasmine-ajax');

  const ArtifactStoresWidget = require("views/artifact_stores/artifact_stores_widget");

  let $root, root;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });

  afterEach(() => {
    window.destroyDomElementForTest();
    m.mount(root, null);
    m.redraw();
  });

  const url            = '/go/api/admin/artifact_stores';
  const pluginInfosUrl = '/go/api/admin/plugin_info?type=artifact';

  describe('Loading Data', () => {

    it('should show error on failure', () => {
      jasmine.Ajax.install();
      jasmine.Ajax.stubRequest(url, undefined, 'GET').andReturn({
        responseText: '',
        status:       500
      });

      m.mount(root, ArtifactStoresWidget);

      expect($(".alert")).toBeInDOM();
    });

    it('should show spinner while data is loading', () => {
      jasmine.Ajax.install();
      jasmine.Ajax.stubRequest(url, undefined, 'GET');

      m.mount(root, ArtifactStoresWidget);

      expect($(".page-spinner")).toBeInDOM();
    });

    it('should show data after response is received', () => {
      jasmine.Ajax.install();
      stubGetArtifactStoresResponse();
      jasmine.Ajax.stubRequest(pluginInfosUrl, undefined, 'GET').andReturn({
        responseText: JSON.stringify({}),
        status:       200
      });

      m.mount(root, ArtifactStoresWidget);

      expect($(".header-panel")).toBeInDOM();
    });
  });

  describe('List', () => {
    it('should disable Add button when no plugins are present', () => {
      jasmine.Ajax.install();
      stubGetArtifactStoresResponse();
      jasmine.Ajax.stubRequest(pluginInfosUrl, undefined, 'GET').andReturn({
        responseText: JSON.stringify({
          "_embedded": {
            "plugin_info": []
          }
        }),
        status:       200
      });

      m.mount(root, ArtifactStoresWidget);

      expect($(".add-artifact-store")).toHaveAttr("disabled");
    });

    it('should enable Add button when plugins are present', () => {
      jasmine.Ajax.install();
      stubGetArtifactStoresResponse();
      stubGetPluginInfosResponse();

      m.mount(root, ArtifactStoresWidget);

      expect($(".add-artifact-store")).not.toHaveAttr("disabled");
    });

    it('should list the artifact stores', () => {
      jasmine.Ajax.install();
      stubGetArtifactStoresResponse();
      stubGetPluginInfosResponse();

      m.mount(root, ArtifactStoresWidget);

      expect($(".collapsible-list-header").length).toEqual(1);
    });
  });

  describe('Operations', () => {

    beforeEach(() => {
      jasmine.Ajax.install();
      stubGetArtifactStoresResponse();
      stubGetPluginInfosResponse();
      m.mount(root, ArtifactStoresWidget);
      m.redraw();
    });

    afterEach(Modal.destroyAll);

    describe('Add', () => {

      it('should show modal when add button is clicked', () => {
        expect($root.find('.reveal:visible')).not.toBeInDOM();
        simulateEvent.simulate($root.find('.add-artifact-store').get(0), 'click');
        m.redraw();
        expect($('.reveal:visible')).toBeInDOM();
        expect($('.reveal:visible input[data-prop-name]')).not.toBeDisabled();
      });

      it('should show modal and render view of first plugin id', () => {
        simulateEvent.simulate($root.find('.add-artifact-store').get(0), 'click');
        m.redraw();

        const pluginId = $('.reveal:visible .modal-body').find('[data-prop-name="pluginId"]').get(0);

        expect($(pluginId).val()).toEqual(dockerRegistryPluginInfoJSON.id);
      });

      it("should allow saving an artifact store if save is successful", () => {
        simulateEvent.simulate($root.find('.add-artifact-store').get(0), 'click');
        m.redraw();
        const modalBody = $('.reveal:visible .modal-body');

        const artifactStoreId = modalBody.find('[data-prop-name="id"]').get(0);
        $(artifactStoreId).val("unit-test");
        simulateEvent.simulate(artifactStoreId, 'input');

        const pluginId = modalBody.find('[data-prop-name="pluginId"]').get(0);
        $(pluginId).val(dockerRegistryPluginInfoJSON.id);
        simulateEvent.simulate(pluginId, 'input');

        m.redraw();

        jasmine.Ajax.stubRequest('/go/api/admin/artifact_stores', undefined, 'POST').andReturn({
          responseText: JSON.stringify({data: dockerRegistryArtifactStoreJSON}),
          status:       200
        });

        simulateEvent.simulate($('.reveal:visible .modal-buttons').find('.save').get(0), 'click');

        m.redraw();

        const refreshRequest = jasmine.Ajax.requests.mostRecent();
        expect(refreshRequest.url).toBe('/go/api/admin/artifact_stores');
        expect(refreshRequest.method).toBe('GET');

        const request = jasmine.Ajax.requests.at(jasmine.Ajax.requests.count() - 2);
        expect(request.url).toBe('/go/api/admin/artifact_stores');
        expect(request.method).toBe('POST');

        expect($('.success')).toContainText('The artifact store unit-test was created successfully');
      });

      it("should change plugin view template in modal on change of plugin from dropdown", () => {
        simulateEvent.simulate($root.find('.add-artifact-store').get(0), 'click');
        m.redraw();

        expect($('.reveal .modal-body input[data-prop-name]')).not.toBeDisabled();
        expect($('.reveal .modal-body [data-prop-name=pluginId] option:selected').text()).toEqual(dockerRegistryPluginInfoJSON.about.name);
        expect($('.reveal .modal-body div.docker_registry_config').text()).toEqual("Docker Registry Url:");

        $('.reveal [data-prop-name=pluginId]').val("cd.go.example.artifactory");
        simulateEvent.simulate($('.reveal [data-prop-name=pluginId]').get(0), 'change');
        m.redraw();

        expect($('.reveal input[data-prop-name]')).not.toBeDisabled();
        expect($('.reveal .modal-body [data-prop-name=pluginId] option:selected').text()).toEqual(artifactoryPluginInfoJSON.about.name);
        expect($('.reveal .modal-body div.artifactory_store_config').text()).toEqual("Example");
      });

    });

    describe('Edit', () => {

      it("should show modal to allow editing an artifact store", () => {
        jasmine.Ajax.stubRequest(`/go/api/admin/artifact_stores/${dockerRegistryArtifactStoreJSON.id}`, undefined, 'GET').andReturn({
          responseText:    JSON.stringify(dockerRegistryArtifactStoreJSON),
          responseHeaders: {
            'ETag': '"foo"'
          },
          status:          200
        });
        expect($root.find('.reveal:visible')).not.toBeInDOM();

        simulateEvent.simulate($root.find('.edit-button').get(0), 'click');
        m.redraw();
        expect($('.reveal:visible')).toBeInDOM();
        expect($('.reveal:visible input[data-prop-name]')).toBeDisabled();
      });

      it("should display error message if fetching an artifact store fails", () => {
        jasmine.Ajax.stubRequest(`/go/api/admin/artifact_stores/${dockerRegistryArtifactStoreJSON.id}`, undefined, 'GET').andReturn({
          responseText: JSON.stringify({message: 'Boom!'}),
          status:       400
        });

        simulateEvent.simulate($root.find('.edit-button').get(0), 'click');
        m.redraw();

        expect($('.alert')).toContainText('Boom!');
      });

      it("should keep the artifact store expanded while edit modal is open", () => {
        jasmine.Ajax.stubRequest(`/go/api/admin/artifact_stores/${dockerRegistryArtifactStoreJSON.id}`, undefined, 'GET').andReturn({
          responseText: JSON.stringify(dockerRegistryArtifactStoreJSON),
          status:       200
        });

        expect($root.find('.plugin-config-read-only')).not.toHaveClass('show');
        simulateEvent.simulate($root.find('.collapsible-list-header').get(0), 'click');
        m.redraw();
        expect($root.find('.plugin-config-read-only')).toHaveClass('show');

        simulateEvent.simulate($root.find('.edit-button').get(0), 'click');
        m.redraw();
        simulateEvent.simulate($('.new-modal-container').find('.reveal:visible .close-button span').get(0), 'click');
        m.redraw();
        expect($root.find('.plugin-config-read-only')).toHaveClass('show');
      });

      it('should show spinner and disable save button while artifact store is being loaded', () => {
        jasmine.Ajax.stubRequest(`/go/api/admin/artifact_stores/${dockerRegistryArtifactStoreJSON.id}`, undefined, 'GET');
        simulateEvent.simulate($root.find('.edit-button').get(0), 'click');
        m.redraw();
        expect($('.reveal:visible .page-spinner')).toBeInDOM();
        expect($('.reveal:visible .modal-buttons').find('.save').get(0)).toHaveAttr('disabled');
      });
    });

    describe('Clone', () => {

      it("should show modal with artifact store", () => {
        jasmine.Ajax.stubRequest(`/go/api/admin/artifact_stores/${dockerRegistryArtifactStoreJSON.id}`, undefined, 'GET').andReturn({
          responseText:    JSON.stringify(dockerRegistryArtifactStoreJSON),
          status:          200,
          responseHeaders: {
            'ETag': '"foo"'
          }
        });
        expect($root.find('.reveal:visible')).not.toBeInDOM();

        simulateEvent.simulate($root.find('.clone-button').get(0), 'click');

        m.redraw();
        expect($('.reveal:visible')).toBeInDOM();
        expect($('.reveal:visible input[data-prop-name]')).not.toBeDisabled();
      });

      it("should display error message if fetching an artifact store fails", () => {
        jasmine.Ajax.stubRequest(`/go/api/admin/artifact_stores/${dockerRegistryArtifactStoreJSON.id}`, undefined, 'GET').andReturn({
          responseText: JSON.stringify({message: 'Boom!'}),
          status:       400
        });

        simulateEvent.simulate($root.find('.clone-button').get(0), 'click');
        m.redraw();

        expect($('.alert')).toContainText('Boom!');
      });

      it("should allow cloning an artifact store", () => {
        jasmine.Ajax.stubRequest(`/go/api/admin/artifact_stores/${dockerRegistryArtifactStoreJSON.id}`, undefined, 'GET').andReturn({
          responseText:    JSON.stringify(dockerRegistryArtifactStoreJSON),
          status:          200,
          responseHeaders: {
            'ETag':         '"foo"',
            'Content-Type': 'application/json'
          }
        });

        simulateEvent.simulate($root.find('.clone-button').get(0), 'click');
        m.redraw();

        const storeId = $('.reveal:visible .modal-body').find('[data-prop-name="id"]').get(0);
        $(storeId).val("foo-clone");
        simulateEvent.simulate(storeId, 'input');

        jasmine.Ajax.stubRequest('/go/api/admin/artifact_stores', undefined, 'POST').andReturn({
          responseText: JSON.stringify({data: dockerRegistryArtifactStoreJSON}),
          status:       200
        });

        simulateEvent.simulate($('.reveal:visible .modal-buttons').find('.save').get(0), 'click');

        const request = jasmine.Ajax.requests.at(jasmine.Ajax.requests.count() - 2);
        expect(request.url).toBe('/go/api/admin/artifact_stores');
        expect(request.method).toBe('POST');

        expect($('.success')).toContainText('The artifact store foo-clone was cloned successfully');
      });

      it('should show spinner and disable save button while artifact store is being loaded', () => {
        jasmine.Ajax.stubRequest(`/go/api/admin/artifact_stores/${dockerRegistryArtifactStoreJSON.id}`, undefined, 'GET');
        simulateEvent.simulate($root.find('.clone-button').get(0), 'click');
        m.redraw();
        expect($('.reveal:visible .page-spinner')).toBeInDOM();
        expect($('.reveal:visible .modal-buttons').find('.save').get(0)).toHaveAttr('disabled');
      });
    });
  });

  function stubGetArtifactStoresResponse() {
    jasmine.Ajax.stubRequest(url, undefined, 'GET').andReturn({
      responseText: JSON.stringify(sampleResponse),
      status:       200
    });
  }

  function stubGetPluginInfosResponse() {
    jasmine.Ajax.stubRequest(pluginInfosUrl, undefined, 'GET').andReturn({
      responseText: JSON.stringify({
        "_embedded": {
          "plugin_info": [dockerRegistryPluginInfoJSON, artifactoryPluginInfoJSON]
        }
      }),
      status:       200
    });
  }

  const dockerRegistryArtifactStoreJSON = {
    "id":         "unit-test",
    "plugin_id":  "cd.go.artifact.docker.registry",
    "properties": [
      {"key": "RegistryURL", "value": "test"},
      {"key": "Username", "value": "foo"},
      {"key": "Password", "value": "bar"}]
  };

  const dockerRegistryPluginInfoJSON = {
    "_links":               {
      "self":  {
        "href": "http://localhost:8153/go/api/admin/plugin_info/cd.go.artifact.docker.registry"
      },
      "doc":   {
        "href": "https://api.gocd.org/#plugin-info"
      },
      "find":  {
        "href": "http://localhost:8153/go/api/admin/plugin_info/:plugin_id"
      },
      "image": {
        "href": "http://localhost:8153/go/api/plugin_images/cd.go.artifact.docker.registry/e1ce6a7746cdab85ec2229424463c48cc15b761459dd85202acbc12693787aff"
      }
    },
    "id":                   "cd.go.artifact.docker.registry",
    "status":               {
      "state": "active"
    },
    "plugin_file_location": "/Users/akshayd/projects/go/gocd/server/plugins/external/docker-registry-artifact-plugin-0.0.1-15.jar",
    "bundled_plugin":       false,
    "about":                {
      "name":                     "Artifact plugin for docker",
      "version":                  "0.0.1-15",
      "target_go_version":        "18.1.0",
      "description":              "Plugin allows to push/pull docker image from public or private docker registry",
      "target_operating_systems": [],
      "vendor":                   {
        "name": "GoCD Contributors",
        "url":  "https://github.com/gocd/docker-artifact-plugin"
      }
    },
    "extensions":           [
      {
        "type":                     "artifact",
        "capabilities":             {},
        "store_config_settings":    {
          "configurations": [
            {
              "key":      "RegistryURL",
              "metadata": {
                "secure":   false,
                "required": true
              }
            },
            {
              "key":      "Username",
              "metadata": {
                "secure":   false,
                "required": true
              }
            },
            {
              "key":      "Password",
              "metadata": {
                "secure":   true,
                "required": true
              }
            }
          ],
          "view":           {
            "template": "<div class='docker_registry_config'>Docker Registry Url:</div>"
          }
        },
        "artifact_config_settings": {
          "configurations": [
            {
              "key":      "BuildFile",
              "metadata": {
                "secure":   false,
                "required": true
              }
            }
          ],
          "view":           {
            "template": "<div></div>"
          }
        },
        "fetch_artifact_settings":  {
          "configurations": [],
          "view":           {
            "template": "<div></div>"
          }
        }
      }
    ]
  };

  const artifactoryPluginInfoJSON = {
    "_links":               {
      "self":  {
        "href": "http://localhost:8153/go/api/admin/plugin_info/cd.go.example.artifactory"
      },
      "doc":   {
        "href": "https://api.gocd.org/#plugin-info"
      },
      "find":  {
        "href": "http://localhost:8153/go/api/admin/plugin_info/:plugin_id"
      },
      "image": {
        "href": "http://localhost:8153/go/api/plugin_images/cd.go.example.artifactory/e1ce6a7746cdab85ec2229424463c48cc15b761459dd85202acbc12693787aff"
      }
    },
    "id":                   "cd.go.example.artifactory",
    "status":               {
      "state": "active"
    },
    "plugin_file_location": "/Users/akshayd/projects/go/gocd/server/plugins/external/artifactory-plugin-0.0.1-15.jar",
    "bundled_plugin":       false,
    "about":                {
      "name":                     "Artifact plugin for Artifactory",
      "version":                  "0.0.1-1",
      "target_go_version":        "18.1.0",
      "description":              "Plugin allows to push/pull docker image from public or private docker registry",
      "target_operating_systems": [],
      "vendor":                   {
        "name": "GoCD Contributors",
        "url":  "https://github.com/gocd/docker-artifact-plugin"
      }
    },
    "extensions":           [
      {
        "type":                     "artifact",
        "capabilities":             {},
        "store_config_settings":    {
          "configurations": [
            {
              "key":      "RepositoryURL",
              "metadata": {
                "secure":   false,
                "required": true
              }
            },
            {
              "key":      "Username",
              "metadata": {
                "secure":   false,
                "required": true
              }
            },
            {
              "key":      "Password",
              "metadata": {
                "secure":   true,
                "required": true
              }
            }
          ],
          "view":           {
            "template": "<div class='artifactory_store_config'>Example</div>"
          }
        },
        "artifact_config_settings": {
          "configurations": [
            {
              "key":      "BuildFile",
              "metadata": {
                "secure":   false,
                "required": true
              }
            }
          ],
          "view":           {
            "template": "<div class='artifactory_config>'>Example1</div>"
          }
        },
        "fetch_artifact_settings":  {
          "configurations": [],
          "view":           {
            "template": "<div></div>"
          }
        }
      }
    ]
  };

  const sampleResponse = {
    "_links":    {
      "self": {
        "href": "http://localhost:8253/go/api/admin/artifact_stores"
      },
      "doc":  {
        "href": "https://api.gocd.org/current/#artifact_stores"
      },
      "find": {
        "href": "http://localhost:8253/go/api/admin/artifact_stores/:id"
      }
    },
    "_embedded": {
      "artifact_stores": [
        dockerRegistryArtifactStoreJSON
      ]
    }
  };
});
