package org.bedone.model

import net.liftweb.http.S
import net.liftweb.util.FieldError
import net.liftweb.record.BaseField

trait MyValidation
{
    def isAlphaNumeric(field: BaseField)(value: String): List[FieldError] = {

        val isOK = value.forall { c =>
            (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') ||
            (c >= '0' && c <= '9') || (c == '_')
        }

        isOK match {
            case true  => Nil
            case false => List(
                FieldError(field, S.?("Only alphanumeric characters and underscore allowed."))
            )
        }
    }
}
