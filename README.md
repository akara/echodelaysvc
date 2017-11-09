# echodelaysvc

The echodelaysvc is a service used for simulating a downstream service with configurable response time characteristics.
The response times are calibrated to be as close as the desired characteristics as possible.
This is useful in resiliency test situations where downstream services become faulty or non-responsive. A resilient
service will be able to serve the same amount of work without toppling even when the downstream services get slower.

## API

### Controlled-Time Echo

**Request**: GET `/echodelaysvc/echo/{echostring}`

**Response**:

```
{
  "path" : "{echostring}",
  "planned-delay" : 1726,
  "real-delay" : 1717
}
```

**Synopsis**:

Echos the `{echostring}` path element back as part of a JSON response. In addition, the injected delay and the actual
resulting delay are provided as part of the response in the field `"planned-delay"` and `"real-delay"`, consequently.

**Example**:

```
GET /echodelaysvc/echo/foo
{
  "path" : "foo",
  "planned-delay" : 1726,
  "real-delay" : 1717
}
```

### Set Time Distribution to Negative Exponential

**Request**: GET `/echodelaysvc/delay/ne?min={mintime}&mean={meantime}&max={maxtime}&truncate={trunc}`

**Response**:

```
{
  "type" : "NegativeExponential",
  "min" : "{mintime} {unit}",
  "mean" : "{meantime} {unit}",
  "max" : "{maxtime} {unit}",
  "truncate" : {true/false}
}
```

**Synopsis**:

Sets the response time characteristics to follow a negative exponential distribution with the given min,
mean, and max time. The `truncate` parameter is a boolean, optional, and defaults to `false`. When truncate is
set to `false`, the distribution is shifted to start at the given `min`, which should be the theoretical lower
limit of the response time for the service it should emulate. The curve continues naturally up to `max`. All
sampled response times above `max` are set to `max`. When truncate is set to `true`, the distribution starts at 0. Any
sampled response times below `min` are set to `min`. All sampled response times above `max` are set to `max`. In this
mode, a large number of the lower sampled response times may be set to `min` as a result of the lower truncation.

**Example**:

```
GET /echodelaysvc/delay/ne?min=50ms&mean=200ms&max=2s
{
  "type" : "NegativeExponential",
  "min" : "50 milliseconds",
  "mean" : "200 milliseconds",
  "max" : "2 seconds",
  "truncate" : false
}
```

###Set Time Distribution to Gaussian

**Request**: GET `echodelaysvc/delay/gaussian?min={mintime}&mean={meantime}&max={maxtime}&sigma={sigmatime}`

**Response**:

```
{
  "type" : "Gaussian",
  "min" : "{mintime} {unit}",
  "mean" : "{meantime} {unit}",
  "max" : "{maxtime} {unit}",
  "sigma" : "{sigmatime} {unit}"
}
```

**Synopsis**:

Sets the response time characteristics to follow a gaussian distribution with the given `min`, `mean`, and `max` time,
and the standard deviation `sigma`. Sampled response times below `min` are set to `min`. Sampled response times above
`max` are set to `max`.

**Example**:

```
GET /echodelaysvc/delay/gaussian?min=50ms&mean=1s&max=2s&sigma=330ms
{
  "type" : "Gaussian",
  "min" : "50 milliseconds",
  "mean" : "1 second",
  "max" : "2 seconds",
  "sigma" : "330 milliseconds"
}
```

###Set Time Distribution to Uniform

**Request**: GET `/echodelaysvc/delay/uniform?min={mintime}&max={maxtime}`

**Response**:

```
{
  "type" : "Uniform",
  "min" : "{mintime} {unit}",
  "max" : "{maxtime} {unit}"
}
```

**Synopsis**:

Sets the response time characteristics to follow a uniform distribution between the given `min` and `max` time.
Since this is a uniform distribution, no truncation of sampled times are needed.

**Example**:

```
GET /echodelaysvc/delay/uniform?min=30ms&max=1s
{
  "type" : "Uniform",
  "min" : "30 milliseconds",
  "max" : "1 second"
}
```

### Get the Current Delay Compensation Time

**Request**: GET `/echodelaysvc/delay/compensate`

**Response**:

```
{
  "total-compensate" : "{time} ms"
}
```

**Synopsis**:

Obtains the current delay compensation time, in milliseconds

**Example**:

```
{
  "total-compensate" : "5.678 ms"
}
```