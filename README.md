# How to Write and Test a Reactive Reader for AWS SQS Using akka, alpakka and Localstack

AWS SQS stands for _Simple Queue Service_. This example explains how to

* connect to SQS via _alpakka_,
* process the messages reactively in via _akka streams_,
* test the pipeline using _Localstack_ and _scalamock_,
* all in scala, of course.