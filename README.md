# How to Write and Test a Reactive Reader for AWS SQS Using akka, alpakka and Localstack

AWS SQS stands for _Simple Queue Service_. This example explains how to

* connect to SQS via _alpakka_,
* process the messages reactively in via _akka streams_,
* test the pipeline using _Localstack_ and _Mockito_,
* all in scala, of course.

For a complete introduction, see the [blog post](https://www.jannikarndt.de/blog/2018/10/reactive_sqs_reader/)!

## Running the Code

### Prerequisites

You need `docker` and the `aws-cli` installed.

### Setup

Run the `start_localstack.sh` script first:

```bash
$ ./run_localstack.sh
```

### Compile & Run

Start `sbt` and `run`!

```bash
$ sbt
sbt:reactive-sqs> run
```

## Testing

Tests need the `localstack` as well. Then start `sbt` and `test`:

```bash
$ sbt
sbt:reactive-sqs> test
```