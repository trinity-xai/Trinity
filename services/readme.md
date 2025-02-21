For all external REST services that Trinity may utilize, it will search in the local services directory for configuration files for those services. By default Trinity looks for a "defaultRestAccessLayer.json" file using a static block prior to any calls.
This configuration object allows local users to customize base URLs and endpoints for various services.

A basic example of this looks like this:

{
  "messageType" : "RestAccessLayerConfig",
  "messageId" : 0,
  "baseRestURL" : "https://my.multimodal.model.org",
  "isAliveEndpoint" : "/embeddings/alive/",
  "imageEmbeddingsEndpoint" : "/embeddings/",
  "notes" : "My happy funtime model that I host"
} 

isAliveEndpoint and imageEmbeddingsEndpoint are keys that Trinity looks for to provide remote embedding vector support for images.
While this is a bit janky for now, it will improve as we expand the number of key named services.