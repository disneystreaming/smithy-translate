/* Copyright 2022 Disney Streaming
 *
 * Licensed under the Tomorrow Open Source Technology License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://disneystreaming.github.io/TOST-1.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package smithyfmt;

import smithyfmt.scala.util.Either;

final public class Result {
	private final String error;
	private final String value;

	private final boolean success;

	public Result(Either<String, String> value) {
		this.success = value.isRight();
		if (this.success) {
			this.value = value.getOrElse(null);
			this.error = null;
		} else {
			this.value = null;
			this.error = value.swap().getOrElse(null);
		}
	}

	public boolean isSuccess() {
		return this.success;
	}

	public String getValue() {
		return this.value;
	}

	public String getError() {
		return this.error;
	}
}