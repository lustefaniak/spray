/*
 * Copyright © 2011-2015 the spray project <http://spray.io>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package spray.http

case class ContentTypeRange(mediaRange: MediaRange, charsetRange: HttpCharsetRange) extends ValueRenderable {
  def matches(contentType: ContentType) =
    mediaRange.matches(contentType.mediaType) && charsetRange.matches(contentType.charset)

  def render[R <: Rendering](r: R): r.type = charsetRange match {
    case HttpCharsetRange.`*` ⇒ r ~~ mediaRange
    case x                    ⇒ r ~~ mediaRange ~~ ContentType.`; charset=` ~~ x
  }
}

object ContentTypeRange {
  val `*` = ContentTypeRange(MediaRanges.`*/*`)

  implicit def apply(mediaType: MediaType): ContentTypeRange = apply(MediaRange(mediaType), HttpCharsetRange.`*`)
  implicit def apply(mediaRange: MediaRange): ContentTypeRange = apply(mediaRange, HttpCharsetRange.`*`)
}

case class ContentType(mediaType: MediaType, definedCharset: Option[HttpCharset]) extends ValueRenderable {
  def render[R <: Rendering](r: R): r.type = definedCharset match {
    case Some(cs) ⇒ r ~~ mediaType ~~ ContentType.`; charset=` ~~ cs
    case _        ⇒ r ~~ mediaType
  }
  def charset: HttpCharset = definedCharset getOrElse HttpCharsets.`ISO-8859-1`

  def isCharsetDefined = definedCharset.isDefined
  def noCharsetDefined = definedCharset.isEmpty

  def withMediaType(mediaType: MediaType) =
    if (mediaType != this.mediaType) copy(mediaType = mediaType) else this
  def withCharset(charset: HttpCharset) =
    if (noCharsetDefined || charset != definedCharset.get) copy(definedCharset = Some(charset)) else this
  def withoutDefinedCharset =
    if (isCharsetDefined) copy(definedCharset = None) else this
}

object ContentType {
  private[http] case object `; charset=` extends SingletonValueRenderable

  def apply(mediaType: MediaType, charset: HttpCharset): ContentType = apply(mediaType, Some(charset))
  implicit def apply(mediaType: MediaType): ContentType = apply(mediaType, None)
}

object ContentTypes {
  @deprecated("Use ContentTypeRange.`*` instead", "1.x-RC3")
  val `*` = ContentTypeRange.`*`

  // RFC4627 defines JSON to always be UTF encoded, we always render JSON to UTF-8
  val `application/json` = ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`)
  val `text/plain` = ContentType(MediaTypes.`text/plain`)
  val `text/plain(UTF-8)` = ContentType(MediaTypes.`text/plain`, HttpCharsets.`UTF-8`)
  val `application/octet-stream` = ContentType(MediaTypes.`application/octet-stream`)

  // used for explicitly suppressing the rendering of Content-Type headers on requests and responses
  val NoContentType = ContentType(MediaTypes.NoMediaType)
}
