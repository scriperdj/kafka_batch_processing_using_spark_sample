package example.spark

import scala.beans.BeanProperty

class Config {
  @BeanProperty var inputTopic: String = _
  @BeanProperty var startOffset: Long = 0L
  @BeanProperty var stopOffset: Long = 0L
  @BeanProperty var kafkaBrokers: String = _
}
