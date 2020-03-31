#!/bin/bash


if [ -n "$1" ]; then # If first parameter passed then print Hi

	BUCKET=$1

else
    echo "No parameter bucket found. "
    exit
fi

sbt clean assembly test

aws s3 cp emr/target/scala-2.11/emr-assembly-0.1.jar s3://$BUCKET

aws s3 cp ec2/target/scala-2.11/ec2-assembly-0.1.jar s3://$BUCKET