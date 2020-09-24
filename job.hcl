// nomad job run -output job.hcl
job "foo" {
  datacenters = [
    "default"]

  group "bar" {
    task "myTask" {
      driver = "raw_exec"
      config {
        command = "/bin/sleep"
        args = [
          "1"]
      }

      //      resources {
      //        memory = 128
      //      }
    }
  }
}