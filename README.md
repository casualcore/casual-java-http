# CASUAL HTTP

## What is it?

It's a JAX-RS application that provides the functionality to make ```TPNOTRAN``` calls to exported casual java services as well as imported casual services.

## Prerequisites

Is dependent on:
* a java application server, such as [wildfly](https://www.wildfly.org/) using a Java 21, or later, vm
* that [casual-jca-app](https://github.com/casualcore/casual-java/tree/3.2) and [casual-caller-app](https://github.com/casualcore/casual-caller/tree/3.2) both are installed

## How to use it?

Deploy the ```casual-http-app``` in your application server.
Issue service calls such as:
```sh
$curl -v -d @curl-data -H content-type:application/casual-x-octet http://10.106.219.132:8080/casual/casual%2fexample%2fecho
```

```casual/example/echo``` being the service in this case.

## Supported content-types

Supports the following content-types:
 * application/casual-x-octet
 * application/json
 * application/casual-field
 * application/casual-string
 
The return content type is deduced from the service response buffer and is also one of the supported content-types.

## Error

Calls resulting in anything but TPSUCCESS:
* ```TPENOENT``` generates 404 ( NOT_FOUND)
* ```TPETIME``` generates 408 ( REQUEST_TIMEOUT)
* Any other error generates a 500 ( INTERNAL_SERVER_ERROR) 

These errors are also logged, as finest, with the service name, service return state and error state.

## Exceptional behaviour

Results in a 500 ( INTERNAL_SERVER_ERROR) and the exception is logged as a WARNING.
