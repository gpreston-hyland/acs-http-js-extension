# ACS HTTP JavaScript Extension

This extension will allow Alfresco Content Services (ACS) JavaScript to make http calls. It is based on the original acs-invoke-http-calls projects in the References. When deployed, the extension creates a JavaScript root level object named ***httpse*** in ACS.

### Use-Case / Requirement

An HTTP call (GET, POST, PUT etc) has to be triggered from ACS. The referenced projects provide these calls using Basic Authentication (username / password) only. 

This updated version implements the AWS signature authentication for the HTTP type request. Currently it implements only a new `postaws()` method to work with the Amazon AWS Access Key and Secret Key.

### Prerequisites to run this demo end-2-end

* Alfresco Content Services (Version 7.2 and above and compiled with Java 11)  
*For Alfresco Demo Platform up through 7.4.0 have Java 11 in the content container, so make sure to build the jars with Java 11!*

* [acs-http-js-1.0.0.jar extension](assets/acs-http-js-1.0.0.jar)  
*NOTE: This is manually copied after project build to avoid changing OOTB pom.xml config's.*


## Configuration Steps

1. Deploy the [acs-http-js-1.0.0.jar](assets/acs-http-js-1.0.0.jar) file to ACS. Full credits and thanks to [Rui Fernandes](https://github.com/rjmfernandes), [Sherry Matthews](https://github.com/sherrymax/), and [Olufemi Okanlomo](https://github.com/ookanlomo) for their previous ideas and contributions this is building upon.
2. Restart ACS Server/Container.

## JavaScript examples that invoke HTTP with Basic Authentication

```javascript
var requestBody = '{ "Id": 78912,  "Customer": "Jason Sweet", "Quantity": 1,  "Price": 18.00 }';
var r = httpse.post(requestURL, requestBody, "application/json",'myuser','mypassword');
print(r);
```

```javascript
var requestBody = '{ "id": "9909", "name": "Sam Jackson M.D", "address": "123 Sample Ave, Harford, CT 08661"}';
var r = httpse.post('http://ec2-1-2-3-4.compute-1.amazonaws.com:4000/doctors', requestBody, "application/json", "uname", "pw");
logger.error(r);
```

```javascript
try {
    var hostName = 'http://' + sysAdmin.getHost();
	var requestBody = '{ "id": "9909", "name": "Sam Jackson M.D", "address": "123 Sample Ave, Harford, CT 08661"}';

    var res = httpse.post(requestURL, requestBody, "application/json", 'uname', 'pw');
    logger.log(res);

} catch (ex) {
	logger.error(ex);
}
```

```javascript
var requestBody = '{"name":"Passport"}';
var r = httpse.put(requestURL, requestBody, "application/json", "uname", "pw");
logger.error(r);
```

## JavaScript examples using AWS Access / Secret Keys

The new `postaws()` method will require two JSON strings as parameters; the request body and the AWS Authentication properties. In the example below is for Comprehend's Detect Sentiment.

*Request Body*

```json 
{
    "LanguageCode": "en",
    "Text": "I called for a quote. They told me they would show up the next day to provide the quote. I waited and they never showed up or provided any explanation for not showing or any further follow up. Very unprofessional and unreliable company. 0/5. Would not recommend."
}
```

*AWS Authentication properties*

```json
{
    "accessKey": "ThisIsMyAccessKey",
    "secretKey": "ThisIsMySecretKey",
    "region": "us-east-1",
    "service": "comprehend",
    "amzTarget": "Comprehend_20171127.DetectSentiment"
}
```

**When calling from JavaScript, remember to `stringify()` or just create it as a single string value.**

The following sets the description of the target document to the Amazon Comprehend call's response.

```javascript
var requestbody = '{ "LanguageCode": "en","Text": "I called for a quote. They told me they would show up the next day to provide the quote. I waited and they never showed up or provided any explanation for not showing or any further follow up. Very unprofessional and unreliable company. 0/5. Would not recommend."}';
var awsAuth = '{ "accessKey": "ThisIsMyAccessKey","secretKey": "ThisIsMySecretKey","region": "us-east-1","service": "comprehend","amzTarget": "Comprehend_20171127.DetectSentiment"}';
var compUrl = 'https://comprehend.us-east-1.amazonaws.com';

var r = httpse.postaws(compUrl,requestbody,"",awsAuth);

document.properties["cm:description"] = r;
document.save();
```

#### Where to find the *service* and *amzTarget* values for the call

One option is to look at the [boto github project](https://github.com/boto/botocore/tree/develop/botocore/data). Review the `service-2.json` file.

For the [Comprehend service](https://github.com/boto/botocore/blob/develop/botocore/data/comprehend/2017-11-27/service-2.json) in the example above, the metadata key has part of the information.

```json
{
  "version":"2.0",
  "metadata":{
    "apiVersion":"2017-11-27",
    **"endpointPrefix":"comprehend",**
    "jsonVersion":"1.1",
    "protocol":"json",
    "serviceFullName":"Amazon Comprehend",
    "serviceId":"Comprehend",
    "signatureVersion":"v4",
    "signingName":"comprehend",
    "targetPrefix":"Comprehend_20171127",
    "uid":"comprehend-2017-11-27"
...
}
```

The `endpointPrefix` should correspond to the `service` key. The `amzTarget` value '`Comprehend_20171127.DetectSentiment`' is the `targetPrefix`, a period, and the corresponding Action's `name`, as shown below.

```json
    "DetectSentiment":{
      "name":"DetectSentiment",
      "http":{
        "method":"POST",
        "requestUri":"/"
      },
      "input":{"shape":"DetectSentimentRequest"},
      "output":{"shape":"DetectSentimentResponse"},
      "errors":[
        {"shape":"InvalidRequestException"},
        {"shape":"TextSizeLimitExceededException"},
        {"shape":"UnsupportedLanguageException"},
        {"shape":"InternalServerException"}
      ],
      "documentation":"<p>Inspects text and returns an inference of the prevailing sentiment (<code>POSITIVE</code>, <code>NEUTRAL</code>, <code>MIXED</code>, or <code>NEGATIVE</code>). </p>"
    }
```



# References
* <https://github.com/sherrymax/acs-examples/tree/master/acs-invoke-http-calls>
* <https://github.com/ookanlomo/ACS-Depot/tree/main/acs-invoke-http-calls>
* <https://docs.aws.amazon.com/IAM/latest/UserGuide/create-signed-request.html>
* <https://docs.aws.amazon.com/AmazonS3/latest/API/sig-v4-authenticating-requests.html>
* <https://docs.aws.amazon.com/AmazonS3/latest/API/sig-v4-examples-using-sdks.html#sig-v4-examples-using-sdk-java>

